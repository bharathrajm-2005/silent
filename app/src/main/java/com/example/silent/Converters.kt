package com.example.silent

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ScheduleTypeConverters {
    @TypeConverter
    @JvmStatic
    fun fromListToIntArray(list: List<Int>?): String? {
        return Gson().toJson(list)
    }

    @TypeConverter
    @JvmStatic
    fun toListFromString(value: String?): List<Int>? {
        val listType = object : TypeToken<List<Int>>() {}.type
        return Gson().fromJson(value, listType)
    }
}