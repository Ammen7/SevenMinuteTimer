package com.example.sevenminutetimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object TimerCore {
    const val INTERVAL_MS = 7 * 60 * 1000L
    private const val PREFS = "timer_prefs"
    private const val KEY_ACTIVE = "active"
    private const val KEY_DUE = "due_at"

    fun isActive(context: Context) = prefs(context).getBoolean(KEY_ACTIVE, false)
    fun dueAt(context: Context) = prefs(context).getLong(KEY_DUE, 0L)

    fun start(context: Context) {
        prefs(context).edit().putBoolean(KEY_ACTIVE, true).apply()
        scheduleNext(context)
    }

    fun stop(context: Context) {
        prefs(context).edit().putBoolean(KEY_ACTIVE, false).putLong(KEY_DUE, 0L).apply()
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 700, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
    }

    fun scheduleNext(context: Context) {
        if (!isActive(context)) return
        val whenAt = System.currentTimeMillis() + INTERVAL_MS
        prefs(context).edit().putLong(KEY_DUE, whenAt).apply()
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, 700, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && manager.canScheduleExactAlarms()) {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pi)
        } else {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenAt, pi)
        }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
