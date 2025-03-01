package com.example.silent

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var noScheduleText: TextView
    private lateinit var scheduleAdapter: ScheduleAdapter
    private val scheduleList = mutableListOf<Schedule>()
    private lateinit var db: ScheduleDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = ScheduleDatabase.getDatabase(applicationContext)
        recyclerView = findViewById(R.id.recycler_schedules)
        noScheduleText = findViewById(R.id.tv_no_schedule)

        scheduleAdapter = ScheduleAdapter(scheduleList)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = scheduleAdapter
        }

        findViewById<FloatingActionButton>(R.id.fab_add).setOnClickListener {
            startActivityForResult(Intent(this, AddScheduleActivity::class.java), CREATE_SCHEDULE_REQUEST)
        }

        loadSchedules()
    }

    private fun loadSchedules() {
        lifecycleScope.launch {
            val schedules = db.scheduleDao().getAllSchedules()
            withContext(Dispatchers.Main) {
                scheduleList.clear()
                scheduleList.addAll(schedules)
                scheduleAdapter.notifyDataSetChanged()
                updateScheduleVisibility()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CREATE_SCHEDULE_REQUEST && resultCode == RESULT_OK) {
            data?.getParcelableExtra<Schedule>("schedule")?.let { newSchedule ->
                lifecycleScope.launch {
                    db.scheduleDao().insert(newSchedule)
                    withContext(Dispatchers.Main) {
                        scheduleList.add(newSchedule)
                        scheduleAdapter.notifyItemInserted(scheduleList.size - 1)
                        scheduleSchedule(newSchedule) // Schedule the action
                        updateScheduleVisibility()
                    }
                }
            }
        }
    }

    private fun scheduleSchedule(schedule: Schedule) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, SilentModeReceiver::class.java).apply {
            putExtra("schedule", schedule)
            putExtra("action", if (schedule.isVibrate) "VIBRATE" else "SILENT")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            schedule.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            schedule.startTime,
            pendingIntent
        )

        val endIntent = Intent(this, SilentModeReceiver::class.java).apply {
            putExtra("schedule", schedule)
            putExtra("action", "NORMAL")
        }
        val endPendingIntent = PendingIntent.getBroadcast(
            this,
            (schedule.id + 1000).toInt(), // Unique request code for end
            endIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            schedule.endTime,
            endPendingIntent
        )
    }

    private fun updateScheduleVisibility() {
        if (scheduleList.isEmpty()) {
            noScheduleText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            noScheduleText.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    companion object {
        const val CREATE_SCHEDULE_REQUEST = 1
    }
}