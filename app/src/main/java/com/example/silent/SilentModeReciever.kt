package com.example.silent

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar

class SilentModeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("SilentModeReceiver", "Received intent: ${intent.action}")
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        when (intent.action) {
            "com.example.silent.ACTIVATE_SCHEDULE" -> {
                val isVibrate = intent.getBooleanExtra("is_vibrate", false)
                if (isVibrate) {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                } else {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                }
                rescheduleIfNeeded(context, intent)
            }

            "com.example.silent.DEACTIVATE_SCHEDULE" -> {
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                rescheduleIfNeeded(context, intent)
            }
        }
    }

    private fun rescheduleIfNeeded(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra("schedule_id", -1L)
        val isEveryday = intent.getBooleanExtra("is_everyday", false)
        val timeMillis = intent.getLongExtra("time_millis", 0L)
        val selectedDaysArray = intent.getIntArrayExtra("selected_days")
        val isStartAction = intent.action == "com.example.silent.ACTIVATE_SCHEDULE"
        val requestCode = if (isStartAction) scheduleId.toInt() else (scheduleId + 1000).toInt()

        if (scheduleId != -1L && timeMillis != 0L) {
            if (isEveryday) {
                scheduleForNextDay(context, Intent(intent), requestCode, timeMillis)
            } else if (selectedDaysArray != null && selectedDaysArray.isNotEmpty()) {
                scheduleForNextSelectedDay(
                    context,
                    Intent(intent),
                    requestCode,
                    timeMillis,
                    selectedDaysArray.toList()
                )
            }
        }
    }

    private fun scheduleForNextDay(context: Context, intent: Intent, requestCode: Int, timeMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance()
        val scheduleTime = Calendar.getInstance().apply {
            this.timeInMillis = timeMillis
        }

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, scheduleTime.get(Calendar.HOUR_OF_DAY))
        calendar.set(Calendar.MINUTE, scheduleTime.get(Calendar.MINUTE))
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        scheduleAlarm(context, intent, requestCode, calendar.timeInMillis, alarmManager)

        Log.d("SilentModeReceiver", "Rescheduled for tomorrow at ${SimpleDateFormat("HH:mm").format(calendar.time)}")
    }

    private fun scheduleForNextSelectedDay(
        context: Context,
        intent: Intent,
        requestCode: Int,
        timeMillis: Long,
        selectedDays: List<Int>
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val now = Calendar.getInstance()
        val scheduleTime = Calendar.getInstance().apply {
            timeInMillis = timeMillis
        }

        val baseHour = scheduleTime.get(Calendar.HOUR_OF_DAY)
        val baseMinute = scheduleTime.get(Calendar.MINUTE)

        val targetCalendar = Calendar.getInstance()
        targetCalendar.set(Calendar.HOUR_OF_DAY, baseHour)
        targetCalendar.set(Calendar.MINUTE, baseMinute)
        targetCalendar.set(Calendar.SECOND, 0)
        targetCalendar.set(Calendar.MILLISECOND, 0)

        for (i in 0..6) {
            val tryCalendar = targetCalendar.clone() as Calendar
            tryCalendar.add(Calendar.DAY_OF_YEAR, i)
            val dayToCheck = (tryCalendar.get(Calendar.DAY_OF_WEEK) - 1) % 7 // Sunday = 0

            if (selectedDays.contains(dayToCheck)) {
                if (tryCalendar.timeInMillis > now.timeInMillis) {
                    scheduleAlarm(context, intent, requestCode, tryCalendar.timeInMillis, alarmManager)
                    Log.d("SilentModeReceiver", "Rescheduled for ${SimpleDateFormat("EEEE, HH:mm").format(tryCalendar.time)}")
                    return
                }
            }
        }

        Log.d("SilentModeReceiver", "No upcoming selected day with future time found")
    }

    private fun scheduleAlarm(
        context: Context,
        intent: Intent,
        requestCode: Int,
        timeInMillis: Long,
        alarmManager: AlarmManager
    ) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }
    }

    private fun rescheduleAlarmForTomorrow(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra("schedule_id", -1)
        val isVibrate = intent.getBooleanExtra("is_vibrate", false)
        val isEveryday = intent.getBooleanExtra("is_everyday", false)
        val timeMillis = intent.getLongExtra("time_millis", -1)

        if (!isEveryday || scheduleId == -1L || timeMillis == -1L) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val newTime = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }

        val newIntent = Intent(context, SilentModeReceiver::class.java).apply {
            action = intent.action
            putExtra("schedule_id", scheduleId)
            putExtra("is_vibrate", isVibrate)
            putExtra("is_everyday", isEveryday)
            putExtra("time_millis", newTime.timeInMillis)
        }

        val requestCode = if (intent.action == "com.example.silent.ACTIVATE_SCHEDULE") {
            scheduleId.toInt()
        } else {
            (scheduleId + 1000).toInt()
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            newIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                newTime.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                newTime.timeInMillis,
                pendingIntent
            )
        }

        Log.d("SilentModeReceiver", "Rescheduled alarm for tomorrow at ${SimpleDateFormat("HH:mm").format(newTime.time)}")
    }
}
