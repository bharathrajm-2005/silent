package com.example.silent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ScheduleAdapter(
    private val schedules: List<Schedule>,
    private val onDeleteClick: (Schedule) -> Unit
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    class ScheduleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.tv_schedule_name)
        val timeTextView: TextView = view.findViewById(R.id.tv_schedule_time)
        val modeTextView: TextView = view.findViewById(R.id.tv_schedule_mode)
        val daysTextView: TextView = view.findViewById(R.id.tv_schedule_days)
        val deleteButton: ImageButton = view.findViewById(R.id.btn_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val schedule = schedules[position]

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startTime = sdf.format(Date(schedule.startTime))
        val endTime = sdf.format(Date(schedule.endTime))

        holder.nameTextView.text = schedule.name
        holder.timeTextView.text = "$startTime - $endTime"
        holder.modeTextView.text = if (schedule.isVibrate) "Vibrate" else "Silent"

        holder.daysTextView.text = if (schedule.isEveryday) {
            "Everyday"
        } else {
            val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            schedule.selectedDays.map { dayNames[it] }.joinToString(", ")
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClick(schedule)
        }
    }

    override fun getItemCount() = schedules.size
}