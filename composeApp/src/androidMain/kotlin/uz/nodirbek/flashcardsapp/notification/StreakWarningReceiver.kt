package uz.nodirbek.flashcardsapp.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uz.nodirbek.flashcardsapp.AppContainer
import uz.nodirbek.flashcardsapp.MainActivity
import uz.nodirbek.flashcardsapp.shared.data.local.PreferencesDataStore
import uz.nodirbek.flashcardsapp.shared.scheduler.RateCardUseCase

class StreakWarningReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dataStore = PreferencesDataStore(AppContainer.dataStoreFor(context))
                val lastActive = dataStore.lastActiveDate.first()
                val today = RateCardUseCase.getTodayDate()
                if (lastActive == today) return@launch  // already studied today, no warning needed

                val streak = dataStore.streak.first()
                if (streak <= 0) return@launch  // no streak to protect

                showWarning(context, streak)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showWarning(context: Context, streak: Int) {
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationReceiver.CHANNEL_ID)
            .setContentTitle("⚠️ Серия $streak дней под угрозой!")
            .setContentText("Осталось до полуночи — повтори хоть несколько карточек")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                NotificationReceiver.CHANNEL_ID,
                "Study Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)
        }
        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 1003
    }
}
