package be.moncoachsport

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/** Persistent OS scheduling state. No service or permanent wake lock. */
class RestScheduler(private val context: Context) {
    private val alarms = context.getSystemService(AlarmManager::class.java)
    private val state = context.getSharedPreferences("scheduled-reminders", Context.MODE_PRIVATE)
    fun preciseAllowed() = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()
    private fun pending(id: Int, end: Long) = PendingIntent.getBroadcast(
        context, id, Intent(context, RestReceiver::class.java).putExtra("id", id).putExtra("end", end),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    fun update(end: Long, enabled: Boolean) = synchronized(lock) {
        context.getSystemService(NotificationManager::class.java).cancel(42)
        clearCountdown()
        schedule(42, if (enabled) end else 0, true)
        if (enabled && end > System.currentTimeMillis() && state.getLong("delivered42",0) != end)
            showCountdown(end)
    }
    fun clearCountdown() { context.getSystemService(NotificationManager::class.java).cancel(44) }
    private fun notificationsAllowed() = (Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
        context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    private fun openWorkout() = PendingIntent.getActivity(context,42,
        Intent(context,MainActivity::class.java).putExtra("openWorkout",true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun showCountdown(end: Long) {
        if (!notificationsAllowed()) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("rest_countdown", "Repos en cours", NotificationManager.IMPORTANCE_LOW)
            .apply { enableVibration(false); setSound(null,null) })
        manager.notify(44, Notification.Builder(context,"rest_countdown")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Repos en cours")
            .setContentText("Temps restant · touchez pour reprendre la séance")
            .setWhen(end).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true)
            .setOngoing(true).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_STATUS).setContentIntent(openWorkout())
            .setTimeoutAfter((end-System.currentTimeMillis()).coerceAtLeast(1)).build())
    }
    /** The VM calls this at the deadline, even when system notifications are disabled. */
    fun finishInApp(end: Long, notify: Boolean) = synchronized(lock) {
        if (end <= 0 || System.currentTimeMillis() < end || state.getLong("delivered42",0) == end) return@synchronized
        val scheduled = state.getLong("end42",0)
        if (scheduled != 0L && scheduled != end) return@synchronized
        state.edit().putLong("end42",end).commit()
        completeRest(end, notify)
    }
    private fun completeRest(end: Long, notify: Boolean) {
        alarms.cancel(pending(42,end))
        clearCountdown()
        state.edit().putLong("end42",0).putLong("delivered42",end).commit()
        if (System.currentTimeMillis() - end > 10 * 60_000) return
        val foreground = AppForeground.visible && context.getSystemService(PowerManager::class.java).isInteractive
        if (foreground && notify) {
            val vibrator = context.getSystemService(Vibrator::class.java)
            if (vibrator.hasVibrator()) vibrator.vibrate(VibrationEffect.createOneShot(150,VibrationEffect.DEFAULT_AMPLITUDE),
                android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).build())
        }
        if (!notify || !notificationsAllowed()) return
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = if (foreground) "rest_done_visible" else "rest"
        manager.createNotificationChannel(NotificationChannel(channel,
            if (foreground) "Repos terminé dans l’application" else "Fin du repos",
            if (foreground) NotificationManager.IMPORTANCE_LOW else NotificationManager.IMPORTANCE_DEFAULT)
            .apply { enableVibration(!foreground) })
        manager.notify(42,Notification.Builder(context,channel)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Ta pause est finie")
            .setContentText("Commence ta prochaine série quand tu te sens prêt.")
            .setContentIntent(openWorkout()).setAutoCancel(true).build())
    }
    fun cycle(start: Long, enabled: Boolean) = synchronized(lock) {
        val end = if (start > 0 && enabled)
            Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).plusDays(56).toInstant().toEpochMilli()
        else 0L
        schedule(43, end, false)
    }
    private fun schedule(id: Int, end: Long, exact: Boolean) {
        alarms.cancel(pending(id, end))
        if (end > 0 && state.getLong("delivered$id",0) == end) return
        state.edit().putLong("end$id", end).commit()
        if (end <= System.currentTimeMillis()) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(
            if (id == 42) "rest" else "cycle", if (id == 42) "Fin du repos" else "Fin du cycle",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { enableVibration(true) })
        if (exact && preciseAllowed()) {
            try {
                // User-visible timer alarm: avoids the idle quota for repeated allow-while-idle alarms.
                val show = PendingIntent.getActivity(context,42,
                    Intent(context,MainActivity::class.java).putExtra("openWorkout",true)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                alarms.setAlarmClock(AlarmManager.AlarmClockInfo(end, show), pending(id, end))
                return
            } catch (_: SecurityException) { /* Permission revoked: inexact fallback. */ }
        }
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pending(id, end))
    }
    /** Rebuild after boot, app update, grant or clock change. Never replay an expired rest. */
    fun reconcile() = synchronized(lock) {
        listOf(42, 43).forEach { id ->
            val end = state.getLong("end$id", 0)
            if (end > System.currentTimeMillis()) schedule(id, end, id == 42)
            else if (id == 42) schedule(id, 0, true)
            else if (end > 0) deliver(id, end)
        }
    }
    fun deliver(id: Int, end: Long) = synchronized(lock) {
        if (id !in listOf(42, 43) || end <= 0 || state.getLong("end$id", 0) != end) return@synchronized
        val now = System.currentTimeMillis()
        if (now < end) return@synchronized
        if (id == 42) {
            completeRest(end, true)
            return@synchronized
        }
        state.edit().putLong("end$id", 0).putLong("delivered$id",end).commit()
        if (id == 42 && now - end > 10 * 60_000) return@synchronized
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            return@synchronized
        val open = PendingIntent.getActivity(context, id,
            Intent(context, MainActivity::class.java).putExtra("openWorkout", id == 42)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = Notification.Builder(context, if (id == 42) "rest" else "cycle")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (id == 42) "Ta pause est finie" else "Ton cycle de huit semaines est terminé")
            .setContentText(if (id == 42) "Commence ta prochaine série quand tu te sens prêt." else "Fais le bilan de ton programme et de ta récupération.")
            .setContentIntent(open).setAutoCancel(true).build()
        context.getSystemService(NotificationManager::class.java).notify(id, notification)
    }
    companion object { private val lock = Any() }
}
class RestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val repository = SportRepository(context)
            try {
                val scheduler = RestScheduler(context)
                val alerts = repository.alerts.first()
                val timer = repository.timer.first()
                if (intent.action != null) {
                    scheduler.update(timer.first, alerts)
                    scheduler.cycle(repository.cycleStart.first(), alerts && repository.personal.first().cycleReminder)
                    scheduler.reconcile()
                } else {
                    val id = intent.getIntExtra("id",42)
                    val end = intent.getLongExtra("end",0)
                    if ((id == 42 && alerts && timer.first == end) ||
                        (id == 43 && alerts && repository.personal.first().cycleReminder)) scheduler.deliver(id,end)
                }
            } catch (_: Exception) {
                // A receiver must finish without crashing the app if storage is temporarily unavailable.
            } finally {
                repository.close()
                pending?.finish()
            }
        }
    }
}
