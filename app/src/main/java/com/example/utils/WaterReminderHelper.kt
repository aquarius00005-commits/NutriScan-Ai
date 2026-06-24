package com.example.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.receiver.WaterReminderReceiver

object WaterReminderHelper {
    private const val CHANNEL_ID = "water_intake_reminders"
    private const val CHANNEL_NAME = "Water Reminders"
    private const val CHANNEL_DESC = "Triggers helpful notification reminders to stay hydrated throughout the day."
    private const val PREFS_NAME = "water_reminder_prefs"
    private const val KEY_REMINDERS_ENABLED = "reminders_enabled"
    private const val KEY_REMINDER_INTERVAL_MIN = "reminder_interval_min"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun isRemindersEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_REMINDERS_ENABLED, false)
    }

    fun getReminderIntervalMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_REMINDER_INTERVAL_MIN, 120) // Default: 2 hours
    }

    fun setRemindersEnabled(context: Context, enabled: Boolean, intervalMinutes: Int = 120) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_REMINDERS_ENABLED, enabled)
            .putInt(KEY_REMINDER_INTERVAL_MIN, intervalMinutes)
            .apply()

        if (enabled) {
            scheduleNextReminder(context, intervalMinutes)
        } else {
            cancelReminders(context)
        }
    }

    fun scheduleNextReminder(context: Context, intervalMinutes: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, WaterReminderReceiver::class.java).apply {
                action = WaterReminderReceiver.ACTION_TRIGGER_REMINDER
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val triggerTime = System.currentTimeMillis() + (intervalMinutes * 60 * 1000L)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("WaterReminderHelper", "Failed to schedule next reminder alarm", e)
        }
    }

    fun cancelReminders(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, WaterReminderReceiver::class.java).apply {
                action = WaterReminderReceiver.ACTION_TRIGGER_REMINDER
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (e: Exception) {
            android.util.Log.e("WaterReminderHelper", "Failed to cancel reminders", e)
        }
    }

    fun triggerInstantTestNotification(context: Context) {
        createNotificationChannel(context)
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = WaterReminderReceiver.ACTION_TRIGGER_REMINDER
        }
        context.sendBroadcast(intent)
    }
}
