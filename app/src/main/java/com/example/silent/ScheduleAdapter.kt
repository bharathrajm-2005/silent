package com.example.silent

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar

class ScheduleAdapter(private val scheduleList: List<Schedule>) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {
    class ScheduleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val scheduleName: TextView = itemView.findViewById(R.id.tv_schedule_name) // TextView for schedule name
        val scheduleTime: TextView = itemView.findViewById(R.id.tv_schedule_time) // TextView for schedule time
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val schedule = scheduleList[position]
        holder.scheduleName.text = schedule.name
        holder.scheduleTime.text = formatTime(schedule.startTime) + " - " + formatTime(schedule.endTime)
    }

    override fun getItemCount(): Int {
        return scheduleList.size // Return the number of items in the list
    }

    private fun formatTime(timeInMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeInMillis
        return String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    }
}