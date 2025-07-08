package com.example.silent

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var noScheduleTextView: TextView
    private lateinit var scheduleAdapter: ScheduleAdapter
    private val scheduleList = mutableListOf<Schedule>()
    private lateinit var db: ScheduleDatabase
    private val ADD_SCHEDULE_REQUEST_CODE = 100
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideStatusBar()
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "SilentModeChannel",
                "Silent Mode Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        requestDndPermission()
        db = ScheduleDatabase.getDatabase(applicationContext)
        recyclerView = findViewById(R.id.recyclerView)
        noScheduleTextView = findViewById(R.id.tv_no_schedule)

        recyclerView.layoutManager = LinearLayoutManager(this)
        scheduleAdapter = ScheduleAdapter(scheduleList) { schedule ->
            showDeleteConfirmationDialog(schedule)
        }
        recyclerView.adapter = scheduleAdapter

        findViewById<FloatingActionButton>(R.id.fab_add).setOnClickListener {
            val intent = Intent(this, AddScheduleActivity::class.java)
            startActivityForResult(intent, ADD_SCHEDULE_REQUEST_CODE)
        }
        requestExactAlarmPermission()
        loadSchedules()
    }
    private fun hideStatusBar() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideStatusBar()
        }
    }
    private fun requestDndPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                // Show an explanation dialog first
                AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("This app needs Do Not Disturb access to silence your phone automatically. Please enable it in the next screen.")
                    .setPositiveButton("OK") { _, _ ->
                        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivityForResult(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM), REQUEST_CODE_REQUEST_EXACT_ALARM)
            }
        }
    }
    private fun loadSchedules() {
        lifecycleScope.launch {
            val schedules = withContext(Dispatchers.IO) { db.scheduleDao().getAllSchedules() }
            withContext(Dispatchers.Main) {
                scheduleList.clear()
                scheduleList.addAll(schedules)
                scheduleAdapter.notifyDataSetChanged()
                updateScheduleVisibility()
            }
        }
    }
    private fun updateScheduleVisibility() {
        if (scheduleList.isEmpty()) {
            recyclerView.visibility = View.GONE
            noScheduleTextView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            noScheduleTextView.visibility = View.GONE
        }
    }
    private fun showDeleteConfirmationDialog(schedule: Schedule) {
        AlertDialog.Builder(this)
            .setTitle("Delete Schedule")
            .setMessage("Are you sure you want to delete this schedule?")
            .setPositiveButton("Delete") { _, _ -> deleteSchedule(schedule) }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun deleteSchedule(schedule: Schedule) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.scheduleDao().deleteScheduleById(schedule.id)
                cancelAlarm(schedule)
            }
            loadSchedules()
        }
    }
    private fun cancelAlarm(schedule: Schedule) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val startIntent = Intent(this, SilentModeReceiver::class.java)
        startIntent.action = "com.example.silent.ACTIVATE_SCHEDULE"
        val startPendingIntent = PendingIntent.getBroadcast(
            this, schedule.id.toInt(), startIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(startPendingIntent)

        val endIntent = Intent(this, SilentModeReceiver::class.java)
        endIntent.action = "com.example.silent.DEACTIVATE_SCHEDULE"
        val endPendingIntent = PendingIntent.getBroadcast(
            this, (schedule.id + 1000).toInt(), endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(endPendingIntent)
    }
    companion object {
        private const val REQUEST_CODE_REQUEST_EXACT_ALARM = 1
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_SCHEDULE_REQUEST_CODE && resultCode == RESULT_OK) {
            val schedule = data?.getParcelableExtra<Schedule>("schedule")
            schedule?.let{
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val id = db.scheduleDao().insert(it)
                        it.id = id
                        scheduleAlarms(it)
                    }
                    loadSchedules()
                }
            }
        }
    }
    private fun scheduleAlarms(schedule: Schedule) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Create calendar instances for start and end times
        val startCalendar = Calendar.getInstance()
        val startTimeCalendar = Calendar.getInstance()
        startTimeCalendar.timeInMillis = schedule.startTime

        startCalendar.set(Calendar.HOUR_OF_DAY, startTimeCalendar.get(Calendar.HOUR_OF_DAY))
        startCalendar.set(Calendar.MINUTE, startTimeCalendar.get(Calendar.MINUTE))
        startCalendar.set(Calendar.SECOND, 0)
        startCalendar.set(Calendar.MILLISECOND, 0)

        val endCalendar = Calendar.getInstance()
        val endTimeCalendar = Calendar.getInstance()
        endTimeCalendar.timeInMillis = schedule.endTime

        endCalendar.set(Calendar.HOUR_OF_DAY, endTimeCalendar.get(Calendar.HOUR_OF_DAY))
        endCalendar.set(Calendar.MINUTE, endTimeCalendar.get(Calendar.MINUTE))
        endCalendar.set(Calendar.SECOND, 0)
        endCalendar.set(Calendar.MILLISECOND, 0)

        // If the time has already passed today, set it for tomorrow
        if (startCalendar.timeInMillis < System.currentTimeMillis()) {
            startCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        if (endCalendar.timeInMillis < System.currentTimeMillis()) {
            endCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Ensure end time is after start time
        if (endCalendar.timeInMillis < startCalendar.timeInMillis) {
            endCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Schedule for today (or tomorrow if time passed) if it's everyday or if today is a selected day
        val shouldScheduleToday = schedule.isEveryday ||
                (!schedule.isEveryday && schedule.selectedDays.contains(
                    (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1) // Convert to 0-based index
                ))

        if (shouldScheduleToday) {
            // Create intents for start and end alarms
            val startIntent = Intent(this, SilentModeReceiver::class.java).apply {
                action = "com.example.silent.ACTIVATE_SCHEDULE"
                putExtra("schedule_id", schedule.id)
                putExtra("is_vibrate", schedule.isVibrate)
                putExtra("is_everyday", schedule.isEveryday)
                putExtra("time_millis", schedule.startTime)
                putExtra("selected_days", schedule.selectedDays.toIntArray())
            }

            val endIntent = Intent(this, SilentModeReceiver::class.java).apply {
                action = "com.example.silent.DEACTIVATE_SCHEDULE"
                putExtra("schedule_id", schedule.id)
                putExtra("is_everyday", schedule.isEveryday)
                putExtra("time_millis", schedule.endTime)
                putExtra("selected_days", schedule.selectedDays.toIntArray())
            }

            val startPendingIntent = PendingIntent.getBroadcast(
                this,
                schedule.id.toInt(),
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val endPendingIntent = PendingIntent.getBroadcast(
                this,
                (schedule.id + 1000).toInt(),
                endIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    startCalendar.timeInMillis,
                    startPendingIntent
                )

                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    endCalendar.timeInMillis,
                    endPendingIntent
                )
            }
            else
            {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    startCalendar.timeInMillis,
                    startPendingIntent
                )

                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    endCalendar.timeInMillis,
                    endPendingIntent
                )
            }
            Log.d("AlarmScheduling", "Scheduled start alarm for ${SimpleDateFormat("HH:mm:ss").format(Date(startCalendar.timeInMillis))}")
            Log.d("AlarmScheduling", "Scheduled end alarm for ${SimpleDateFormat("HH:mm:ss").format(Date(endCalendar.timeInMillis))}")
        }
        else
        {
            val selectedDays = schedule.selectedDays
            if (!schedule.isEveryday && selectedDays.isNotEmpty())
            {
                scheduleForNextSelectedDay(schedule, selectedDays)
            }
        }
    }
    private fun scheduleForNextSelectedDay(schedule: Schedule, selectedDays: List<Int>) {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_WEEK) - 1
        var daysToAdd = 1
        var nextDay = (today + daysToAdd) % 7
        while (!selectedDays.contains(nextDay)) {
            daysToAdd++
            nextDay = (today + daysToAdd) % 7
        }
        Log.d("AlarmScheduling", "Next schedule will be on day $nextDay, in $daysToAdd days")
    }
    private fun setDailyAlarm(alarmManager: AlarmManager, pendingIntent: PendingIntent, timeInMillis: Long){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }
        else
        {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            )
        }
    }
    private fun setWeeklyAlarm(alarmManager: AlarmManager, pendingIntent: PendingIntent, timeInMillis: Long, dayOfWeek: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, Calendar.getInstance().apply { this.timeInMillis = timeInMillis }.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, Calendar.getInstance().apply { this.timeInMillis = timeInMillis }.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (get(Calendar.DAY_OF_WEEK) != dayOfWeek + 1)
            {
                add(Calendar.DAY_OF_YEAR, (dayOfWeek + 1 - get(Calendar.DAY_OF_WEEK) + 7) % 7)
            }
        }
        setDailyAlarm(alarmManager, pendingIntent, calendar.timeInMillis)
    }
    override fun onResume(){
        super.onResume()
        hideStatusBar()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                Toast.makeText(this, "Do Not Disturb access is required for this app to work", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent()
            val packageName = packageName
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }
}