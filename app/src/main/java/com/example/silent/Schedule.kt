package com.example.silent

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
@Parcelize
@Entity(tableName = "schedules")
data class Schedule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val startTime: Long,
    val endTime: Long,
    val isEveryday: Boolean,
    val selectedDays: List<Int>,
    val isVibrate: Boolean
) : Parcelable