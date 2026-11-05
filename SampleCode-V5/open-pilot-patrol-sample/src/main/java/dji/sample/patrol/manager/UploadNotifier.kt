package dji.sample.patrol.manager

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dji.sample.patrol.PluginActivity
import dji.sample.patrol.R
import dji.sample.patrol.data.UploadProgress
import dji.sample.patrol.data.UploadStage

/**
 * 上传进度通知（M3）。
 *
 * 设计意图：以系统通知增强「边飞边同步 / 收尾补传」的可感知性（进行中/完成/失败三态）。
 * Android 13+ 需 `POST_NOTIFICATIONS` 运行时权限；未授权时**静默降级**（不弹通知、不抛异常），
 * 上传流程与浮窗/页面展示不受影响。
 *
 * 依赖：`androidx.core`（随 appcompat 传递依赖），不新增第三方依赖。
 */
object UploadNotifier {

    private const val TAG = "PatrolUploadNotif"
    private const val CHANNEL_ID = "patrol_upload"
    private const val ID_BASE = 4200

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var channelCreated = false

    /** 连接就绪后初始化（创建通知渠道，幂等）。 */
    fun init(context: Context) {
        appContext = context.applicationContext
        ensureChannel()
    }

    /**
     * 按工单更新通知（进行中/完成/失败）。
     *
     * @param progress 该工单上传进度（文件数/字节）。
     * @param stage 上传编排态，决定通知文案与是否持续。
     */
    fun onProgress(jobId: String, progress: UploadProgress, stage: UploadStage) {
        val context = appContext ?: return
        ensureChannel()
        if (!canNotify(context)) {
            Log.i(TAG, "notify skipped (no permission) job=$jobId stage=$stage")
            return
        }
        val title = context.getString(R.string.fw_notif_channel_name)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent(context, jobId))
        when (stage) {
            UploadStage.COMPLETED -> builder
                .setContentText(context.getString(R.string.fw_notif_upload_done))
                .setOngoing(false)
                .setAutoCancel(true)
            UploadStage.FAILED -> builder
                .setContentText(context.getString(R.string.fw_notif_upload_failed))
                .setOngoing(false)
                .setAutoCancel(true)
            else -> {
                val max = progress.total.coerceAtLeast(1)
                builder
                    .setContentText(context.getString(R.string.fw_notif_uploading))
                    .setProgress(max, progress.uploaded.coerceIn(0, max), false)
                    .setOngoing(true)
            }
        }
        try {
            NotificationManagerCompat.from(context).notify(notificationId(jobId), builder.build())
            Log.i(TAG, "notify job=$jobId stage=$stage uploaded=${progress.uploaded}/${progress.total}")
        } catch (error: SecurityException) {
            Log.w(TAG, "notify denied job=$jobId: ${error.message}")
        }
    }

    /** 取消某工单通知（完成/离开任务）。 */
    fun cancel(jobId: String) {
        val context = appContext ?: return
        try {
            NotificationManagerCompat.from(context).cancel(notificationId(jobId))
            Log.i(TAG, "cancel notify job=$jobId")
        } catch (error: Throwable) {
            Log.w(TAG, "cancel notify failed job=$jobId: ${error.message}")
        }
    }

    private fun ensureChannel() {
        val context = appContext ?: return
        if (channelCreated || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.fw_notif_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        manager.createNotificationChannel(channel)
        channelCreated = true
    }

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    private fun openAppIntent(context: Context, jobId: String): PendingIntent {
        val intent = Intent(context, PluginActivity::class.java).apply {
            putExtra(PluginActivity.EXTRA_TAB, PluginActivity.TAB_TRANSFER)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getActivity(context, notificationId(jobId), intent, flags)
    }

    private fun notificationId(jobId: String): Int = ID_BASE + (jobId.hashCode() and 0x0FFF)

    /** 测试/断连辅助：清空 Context（不主动取消已展示通知）。 */
    fun reset() {
        appContext = null
        channelCreated = false
    }
}
