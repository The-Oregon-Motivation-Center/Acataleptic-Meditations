package com.example.acatalepticmeditations.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class NotificationScheduler(private val context: Context) {
    fun scheduleNotification(date: LocalDate, time: LocalTime, message: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // TODO: Handle the case where the permission is not granted
                return
            }
        }

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("message", message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (date.toEpochDay() + time.toSecondOfDay()).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val zonedDateTime = date.atTime(time).atZone(ZoneId.systemDefault())
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, zonedDateTime.toInstant().toEpochMilli(), pendingIntent)
    }
}
