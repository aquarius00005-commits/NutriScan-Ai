package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.WaterLog
import com.example.data.UserProfile
import com.example.utils.WaterReminderHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

class WaterReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_REMINDER = "com.example.receiver.ACTION_TRIGGER_REMINDER"
        const val ACTION_LOG_WATER_QUICK = "com.example.receiver.ACTION_LOG_WATER_QUICK"
        const val NOTIFICATION_ID = 887766
        const val CHANNEL_ID = "water_intake_reminders"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action
        Log.d("WaterReminderReceiver", "Received event action: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                if (WaterReminderHelper.isRemindersEnabled(context)) {
                    val interval = WaterReminderHelper.getReminderIntervalMinutes(context)
                    WaterReminderHelper.scheduleNextReminder(context, interval)
                }
            }
            ACTION_TRIGGER_REMINDER -> {
                val db = AppDatabase.getDatabase(context)
                CoroutineScope(Dispatchers.IO).launch {
                    val profileFlow = db.userProfileDao().getUserProfileFlow()
                    val profile = profileFlow.firstOrNull() ?: UserProfile()
                    val waterGoalMl = (profile.weightKg * 35).toInt().coerceIn(2000, 4000)

                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val todayLogsFlow = db.waterLogDao().getTodayWaterLogsFlow(calendar.timeInMillis)
                    val logs = todayLogsFlow.firstOrNull() ?: emptyList()
                    val totalWaterMl = logs.sumOf { it.amountMl }

                    showNotification(context, totalWaterMl, waterGoalMl)

                    // Self-propagate alarm to run again at selected interval
                    if (WaterReminderHelper.isRemindersEnabled(context)) {
                        val interval = WaterReminderHelper.getReminderIntervalMinutes(context)
                        WaterReminderHelper.scheduleNextReminder(context, interval)
                    }
                }
            }
            ACTION_LOG_WATER_QUICK -> {
                val db = AppDatabase.getDatabase(context)
                CoroutineScope(Dispatchers.IO).launch {
                    db.waterLogDao().insertWaterLog(WaterLog(amountMl = 250))
                    
                    // Dismiss notification
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(NOTIFICATION_ID)

                    launch(Dispatchers.Main) {
                        Toast.makeText(context, "Log successful! Drank 250 mL of water.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showNotification(context: Context, totalDrankMl: Int, goalMl: Int) {
        WaterReminderHelper.createNotificationChannel(context)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            1,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val logIntent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = ACTION_LOG_WATER_QUICK
        }
        val logPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            logIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val progressInfo = if (totalDrankMl >= goalMl) {
            "Superb! Daily goal of $goalMl mL achieved! You are fully hydrated."
        } else {
            "Drank $totalDrankMl mL of $goalMl mL. ${goalMl - totalDrankMl} mL left to goal!"
        }

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.R.mipmap.ic_launcher)
            .setContentTitle("💧 Stay Hydrated Reminder")
            .setContentText(progressInfo)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(mainPendingIntent)
            .setAutoCancel(true)
            .addAction(
                com.example.R.mipmap.ic_launcher,
                "Log +250 mL",
                logPendingIntent
            )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }
}
