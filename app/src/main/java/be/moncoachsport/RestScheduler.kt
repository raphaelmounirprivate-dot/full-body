package be.moncoachsport

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build

class RestScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    private fun pending() =
        PendingIntent.getBroadcast(
            context,
            42,
            Intent(context, RestReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun update(end: Long, enabled: Boolean) {
        alarms.cancel(pending())
        if (enabled && end > System.currentTimeMillis()) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel("rest", "Fin du repos", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply {
                        description = "Rappel local du minuteur de repos"
                        enableVibration(true)
                    }
            )
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pending())
        }
    }
}

class RestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
        )
            return
        val open =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val notification =
            Notification.Builder(context, "rest")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Repos terminé")
                .setContentText("Reprenez quand vous vous sentez prêt.")
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        context.getSystemService(NotificationManager::class.java).notify(42, notification)
    }
}
