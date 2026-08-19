package uz.nodirbek.flashcardsapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object DownloadNotificationHelper {

    private const val CHANNEL_ID = "deck_download_channel"
    private const val NOTIFICATION_ID = 2001

    fun showDownloading(context: Context, deckTitle: String) {
        val manager = manager(context)
        ensureChannel(manager)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Загрузка колоды")
            .setContentText(deckTitle)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun showImporting(context: Context, deckTitle: String) {
        val manager = manager(context)
        ensureChannel(manager)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Импортирование")
            .setContentText(deckTitle)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun showSuccess(context: Context, deckTitle: String, cardCount: Int) {
        val manager = manager(context)
        ensureChannel(manager)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Колода добавлена")
            .setContentText("$deckTitle · $cardCount карточек")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    fun cancel(context: Context) {
        manager(context).cancel(NOTIFICATION_ID)
    }

    private fun manager(context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun ensureChannel(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Загрузка колод",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
    }
}
