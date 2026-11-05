package com.dji.open.pilot.plugin.sampleitem.mapmanager

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.dji.open.pilot.plugin.R
import dji.open.pilot.base.manager.OpenLocation
import dji.open.pilot.base.manager.map.MapDrawerType
import dji.open.pilot.base.manager.map.MapElement
import dji.open.pilot.base.manager.map.MapElementColor
import dji.open.pilot.client.manager.IServerStateListener
import dji.open.pilot.client.manager.RemoteLinkManager
import dji.open.pilot.client.manager.ServerState
import dji.open.pilot.client.manager.ui.MapWidgetManager
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import java.util.UUID

/** Creation ownership settles independently of UI tokens; other Binder callbacks use Task.reply. */
internal class MapElementController(private val context: Context, private val onReport: (String) -> Unit) {
    private val handler = Handler(Looper.getMainLooper())
    private val queue = ElementRequestQueue(object : ElementRequestQueue.Scheduler {
        override fun post(delayMs: Long, action: () -> Unit) { handler.postDelayed({ action() }, delayMs) }
    })
    private val store = ElementOwnershipStore(context)
    private var hasPersistenceFailure = false
    private val ledger = ElementOwnershipLedger(store::load) { ownership ->
        hasPersistenceFailure = !store.save(ownership)
    }
    private val targets = ElementTargets<MapElement>(ledger)
    private val manager = MapWidgetManager.getInstance()
    private var isConnected = false
    private var isClosed = false
    private var isArHidden = false
    private val serverListener = object : IServerStateListener {
        override fun onStateChanged(status: ServerState) {
            handler.post {
                if (!isClosed) {
                    isConnected = status == ServerState.CONNECTED
                    if (!isConnected) {
                        queue.cancel()
                        targets.reconcile(emptyList())
                    }
                }
            }
        }
        override fun onVersionCompatibleError(error: IDJIError) {
            handler.post { if (!isClosed) onReport(errorDetail("connection", null, error)) }
        }
    }

    init { RemoteLinkManager.getInstance().addServerStateListener(serverListener) }

    fun close() {
        RemoteLinkManager.getInstance().removeServerStateListener(serverListener)
        isClosed = true
        queue.cancel()
        handler.removeCallbacksAndMessages(null)
    }

    private inner class Batch(val operation: String) {
        private val lines = mutableListOf<String>()
        fun add(line: String) {
            Log.i(TAG, "$operation $line")
            lines.add(line)
            val storageHint = if (hasPersistenceFailure) {
                "\nerrorCode=STORE_FAILED hint=${text(R.string.map_element_store_failed)}"
            } else ""
            onReport("$operation\n" + lines.joinToString("\n") + storageHint)
        }
        fun outcome(element: MapElement, code: String, hint: String) {
            add("UUID=${element.id} type=${element.drawerType.name} errorCode=$code hint=$hint")
        }
    }

    private fun enqueue(operation: String, start: (ElementRequestQueue.Task, Batch) -> Unit) {
        handler.post {
            if (isClosed) return@post
            if (!isConnected) {
                onReport("$operation errorCode=REMOTE_DISCONNECTED hint=${text(R.string.map_element_disconnected)}")
                return@post
            }
            var batch: Batch? = null
            queue.enqueue(operation, { task ->
                val activeBatch = Batch(operation)
                batch = activeBatch
                start(task, activeBatch)
            }, {
                if (!isClosed) batch?.add("errorCode=REMOTE_DISCONNECTED hint=${text(R.string.map_element_disconnected)}")
                Log.i(TAG, "$operation canceled on disconnect/close")
            })
        }
    }

    fun insert(build: () -> List<MapElement>?) = enqueue("insert") { task, batch ->
        query(task, batch, {
            val missing = creationTracker.availableTypes(ELEMENT_TYPES, targets.owned().values)
            val pending = targets.missingTypes(ELEMENT_TYPES).filter { it !in missing }
            pending.forEach { type ->
                batch.add("UUID=- type=$type errorCode=CREATION_PENDING hint=${text(R.string.map_element_creation_pending)}")
            }
            if (missing.isEmpty()) {
                if (pending.isEmpty()) batch.add(text(R.string.map_element_already_added))
                task.finish()
            } else {
                val elements = build()
                if (elements == null) {
                    batch.add(text(R.string.map_no_location))
                    task.finish()
                } else insertNext(task, batch, elements.filter { it.drawerType.name in missing })
            }
        }, { task.finish() })
    }

    private fun insertNext(task: ElementRequestQueue.Task, batch: Batch, elements: List<MapElement>) {
        val element = elements.firstOrNull() ?: return task.finish()
        val next = { insertNext(task, batch, elements.drop(1)) }
        // Recheck each submission: another controller may have settled or started this type meanwhile.
        if (element.drawerType.name in targets.owned().values) {
            batch.outcome(element, "ALREADY_OWNED", text(R.string.map_element_already_added))
            next()
            return
        }
        val attempt = creationTracker.begin(element.drawerType.name)
        if (attempt == null) {
            batch.outcome(element, "CREATION_PENDING", text(R.string.map_element_creation_pending))
            next()
            return
        }
        val token = task.request {
            // A deadline settles the UI request, not the real creation attempt.
            batch.outcome(element, "TIMEOUT", text(R.string.map_element_creation_pending))
            next()
        }
        manager.insertMapElement(element, object : CommonCallbacks.CompletionCallbackWithParam<UUID> {
            override fun onSuccess(id: UUID) {
                // This handler is not cleared on close. UUID ownership must survive cancellation/timeout.
                creationCallbackHandler.post {
                    if (attempt.success(id) { uuid, type ->
                            if (isClosed) ledger.claim(uuid, type)
                            else {
                                element.id = uuid
                                targets.created(uuid, type, element)
                            }
                        }) {
                        val code = if (hasPersistenceFailure) "STORE_FAILED" else "OK"
                        Log.i(TAG, "insert UUID=$id type=${element.drawerType.name} errorCode=$code hint=ownership callback settled")
                        if (!isClosed) task.reply(token) {
                            batch.outcome(element, "OK", text(R.string.map_element_insert_success, element.name))
                            next()
                        }
                    }
                }
            }
            override fun onFailure(error: IDJIError) {
                creationCallbackHandler.post {
                    if (attempt.failure()) {
                        Log.e(TAG, errorDetail("insert", element, error))
                        if (!isClosed) task.reply(token) {
                            batch.add(errorDetail("insert", element, error))
                            next()
                        }
                    }
                }
            }
        })
    }

    fun query() = enqueue("query") { task, batch ->
        query(task, batch, { elements ->
            batch.add(text(R.string.map_element_query_result, elements.size))
            val resolved = targets.resolved()
            targets.owned().forEach { (id, type) ->
                batch.add("UUID=$id type=$type errorCode=OK hint=" + text(
                    if (id in resolved) R.string.map_element_owned_found else R.string.map_element_owned_pending))
            }
            task.finish()
        }, { task.finish() })
    }

    private fun query(task: ElementRequestQueue.Task, batch: Batch,
                      onSuccess: (List<MapElement>) -> Unit, onFailure: () -> Unit) {
        val token = task.request {
            batch.add("query errorCode=TIMEOUT hint=${text(R.string.map_element_timeout)}")
            onFailure()
        }
        manager.queryMapElement(object : CommonCallbacks.CompletionCallbackWithParam<List<MapElement>> {
            override fun onSuccess(elements: List<MapElement>?) = task.reply(token) {
                // A null payload is not an authoritative empty snapshot, especially during delete verification.
                if (elements == null) {
                    batch.add("query errorCode=EMPTY_RESPONSE hint=${text(R.string.map_element_query_invalid)}")
                    onFailure()
                } else {
                    targets.reconcile(elements.map { ElementTargets.Remote(it.id, it.drawerType.name, it) })
                    onSuccess(elements)
                }
            }
            override fun onFailure(error: IDJIError) = task.reply(token) {
                batch.add(errorDetail("query", null, error))
                onFailure()
            }
        })
    }

    fun update() = enqueue("update") { task, batch ->
        query(task, batch, {
            reportUnresolved(batch)
            updateNext(task, batch, targets.resolved().values.toList())
        }, { task.finish() })
    }

    private fun updateNext(task: ElementRequestQueue.Task, batch: Batch, elements: List<MapElement>) {
        val original = elements.firstOrNull() ?: return task.finish()
        val proposal = copyElement(original).apply { color = MapElementColor.GREEN }
        val next = { updateNext(task, batch, elements.drop(1)) }
        val token = task.request {
            batch.outcome(original, "TIMEOUT", text(R.string.map_element_timeout))
            next()
        }
        manager.updateMapElement(proposal, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() = task.reply(token) {
                targets.updated(proposal.id, proposal.drawerType.name, proposal)
                batch.outcome(proposal, "OK", text(R.string.map_element_update_success, proposal.name))
                next()
            }
            override fun onFailure(error: IDJIError) = task.reply(token) {
                batch.add(errorDetail("update", original, error))
                next()
            }
        })
    }

    fun delete() = enqueue("delete") { task, batch ->
        query(task, batch, {
            reportUnresolved(batch)
            val snapshot = targets.deletionTargets { id, type ->
                // Query can lag a recent insert. A delete needs only the Sample-owned UUID and last known type.
                try {
                    MapElement().apply {
                        this.id = id
                        drawerType = MapDrawerType.valueOf(type)
                    }
                } catch (error: IllegalArgumentException) {
                    batch.add("UUID=$id type=$type errorCode=INVALID_TYPE hint=$error")
                    null
                }
            }
            deleteNext(task, batch, snapshot, mutableListOf())
        }, { task.finish() })
    }

    private fun deleteNext(task: ElementRequestQueue.Task, batch: Batch,
                           remaining: List<MapElement>, submitted: MutableList<MapElement>) {
        val element = remaining.firstOrNull()
        if (element == null) {
            if (submitted.isEmpty()) task.finish()
            else confirmDeletes(task, batch, submitted)
            return
        }
        val next = { deleteNext(task, batch, remaining.drop(1), submitted) }
        val token = task.request {
            batch.outcome(element, "TIMEOUT", text(R.string.map_element_timeout))
            next()
        }
        manager.deleteMapElement(element, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() = task.reply(token) {
                submitted.add(element)
                batch.outcome(element, "SUBMITTED", text(R.string.map_element_delete_submitted))
                next()
            }
            override fun onFailure(error: IDJIError) = task.reply(token) {
                batch.add(errorDetail("delete", element, error))
                next()
            }
        })
    }

    private fun confirmDeletes(task: ElementRequestQueue.Task, batch: Batch, submitted: List<MapElement>) {
        val confirmation = targets.deletion(submitted.map { it.id }.toSet())
        val unconfirmed = submitted.associateBy { it.id }.toMutableMap()
        fun verify() {
            if (!confirmation.canQuery || unconfirmed.isEmpty()) {
                unconfirmed.values.forEach { batch.outcome(it, "UNCONFIRMED", text(R.string.map_element_delete_unconfirmed)) }
                task.finish()
                return
            }
            // Host currentElements may trail the actual mutation by one second.
            task.later(DELETE_QUERY_DELAY_MS) {
                query(task, batch, { elements ->
                    confirmation.observe(elements.map { it.id }.toSet()).forEach { id ->
                        unconfirmed.remove(id)?.let { element ->
                            targets.confirmDeleted(id)
                            batch.outcome(element, "OK", text(R.string.map_element_delete_success, element.name))
                        }
                    }
                    verify()
                }, {
                    confirmation.observe(null)
                    verify()
                })
            }
        }
        verify()
    }

    private fun reportUnresolved(batch: Batch) {
        if (targets.owned().isEmpty()) batch.add(text(R.string.map_element_add_first))
        targets.owned().filterKeys { it !in targets.resolved() }.forEach { (id, type) ->
            batch.add("UUID=$id type=$type errorCode=UNRESOLVED hint=${text(R.string.map_element_owned_pending)}")
        }
    }

    fun toggleAr() = enqueue("ar") { task, batch ->
        val shouldHide = !isArHidden
        val token = task.request {
            batch.add("hideARAnnotations errorCode=TIMEOUT hint=${text(R.string.map_element_timeout)}")
            task.finish()
        }
        manager.hideARAnnotations(shouldHide, object : CommonCallbacks.CompletionCallback {
            override fun onSuccess() = task.reply(token) {
                isArHidden = shouldHide
                batch.add("hideARAnnotations errorCode=SUBMITTED hint=" +
                    text(if (shouldHide) R.string.map_element_ar_hidden else R.string.map_element_ar_shown))
                task.finish()
            }
            override fun onFailure(error: IDJIError) = task.reply(token) {
                batch.add(errorDetail("hideARAnnotations", null, error))
                task.finish()
            }
        })
    }

    private fun copyElement(element: MapElement) = MapElement().apply {
        id = element.id
        drawerType = element.drawerType
        color = element.color
        name = element.name
        setArShow(element.isArShow)
        points = element.points.map { OpenLocation(it.latitude, it.longitude, it.altitude) }.toMutableList()
    }

    private fun errorDetail(operation: String, element: MapElement?, error: IDJIError): String =
        "$operation UUID=${element?.id ?: "-"} type=${element?.drawerType?.name ?: "-"} " +
            "errorCode=${error.errorCode()} innerCode=${error.innerCode()} hint=${error.hint()} " +
            "description=${error.description()} error=$error"

    private fun text(resId: Int, vararg args: Any): String = context.getString(resId, *args)

    private companion object {
        const val TAG = "MapElementController"
        const val DELETE_QUERY_DELAY_MS = 1_200L
        val ELEMENT_TYPES = listOf(MapDrawerType.POLYLINE.name, MapDrawerType.POLYGON.name, MapDrawerType.PIN.name)
        // Process-local: closing/recreating a controller must not release an uncertain creation type.
        val creationTracker = ElementCreationTracker()
        val creationCallbackHandler = Handler(Looper.getMainLooper())
    }
}
