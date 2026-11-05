package dji.sample.patrol.manager

import dji.sample.patrol.data.Job

/**
 * 航线来源装配：把 [Job] 的 `dji.flight-route` 扩展（或旧字段回退）解析为可加载的 [RouteSource]。
 *
 * 只依赖 `data/` 模型，不直接调用 MSDK，也不改 [WaylineManager] 的任何 SDK 调用。
 * 航线下载来源仅采用 Job 现有 `routeFileUrl`（`.kmz` 结尾）；仅存在 `assetId` 而无可下载
 * KMZ 地址时判 [RouteSource.Invalid]，不臆造/拼接下载地址。
 */
object RouteLoader {

    const val EXTENSION_TYPE = "dji.flight-route"
    const val EXTENSION_SCHEMA_VERSION = "1.0"

    private const val STATUS_CLOUD_PROVIDED = "CLOUD_PROVIDED"
    private const val STATUS_NOT_REQUIRED = "NOT_REQUIRED"
    private const val STATUS_INVALID = "INVALID"

    sealed class RouteSource {
        data class CloudProvided(
            val url: String,
            val assetId: String?,
            val fileName: String?,
        ) : RouteSource()

        object NotRequired : RouteSource()

        data class Invalid(val reason: String) : RouteSource()
    }

    fun resolve(job: Job): RouteSource {
        val extension = job.extensions.firstOrNull {
            it.type == EXTENSION_TYPE && it.schemaVersion == EXTENSION_SCHEMA_VERSION
        }
        val payload = extension?.payload
        val status = payload?.routeStatus ?: job.routeStatus.name
        val assetId = payload?.assetId
        val routeUrl = job.routeFileUrl?.takeIf { it.isNotBlank() }
        val fileName = job.routeFileName ?: routeUrl?.let(::fileNameOf)

        return when (status) {
            STATUS_NOT_REQUIRED -> RouteSource.NotRequired
            STATUS_CLOUD_PROVIDED -> {
                when {
                    routeUrl != null && isKmz(routeUrl) ->
                        RouteSource.CloudProvided(routeUrl, assetId, fileName)
                    assetId != null ->
                        RouteSource.Invalid(
                            "Flight route declares assetId=$assetId but no downloadable .kmz routeFileUrl",
                        )
                    else ->
                        RouteSource.Invalid("Flight route is CLOUD_PROVIDED but no usable .kmz routeFileUrl")
                }
            }
            STATUS_INVALID -> RouteSource.Invalid("Flight route status is INVALID")
            else -> RouteSource.Invalid("Unrecognized flight route status: $status")
        }
    }

    /**
     * 以现有 `routeFileUrl` 为下载地址产出补全了 `routeFileName` 的 [Job] 副本，
     * 交给 [WaylineManager.prepare]，不改其任何 SDK 调用。
     */
    fun effectiveJob(job: Job, source: RouteSource.CloudProvided): Job =
        job.copy(
            routeFileUrl = source.url,
            routeFileName = source.fileName ?: job.routeFileName,
        )

    private fun fileNameOf(url: String): String =
        url.substringBefore('?').substringBefore('#').substringAfterLast('/')

    private fun isKmz(url: String): Boolean =
        fileNameOf(url).endsWith(".kmz", ignoreCase = false)
}
