package dji.sample.flighttask.data

enum class TaskSyncState {
    IDLE,
    SYNCING,
    SYNCED,
    ERROR,
}

internal data class JobsCache(
    val jobs: List<Job>,
    val lastSyncTime: Long? = null,
)
