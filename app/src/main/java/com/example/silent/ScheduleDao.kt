package com.example.silent

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules")
    suspend fun getAllSchedules(): List<Schedule>

    @Insert
    suspend fun insert(schedule: Schedule)

    @Delete
    suspend fun delete(schedule: Schedule)
}