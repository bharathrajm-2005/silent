package com.example.silent

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromListInt(list: List<Int>?): String? {
        return list?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toListInt(string: String?): List<Int>? {
        return string?.let {
            val type: Type = object : TypeToken<List<Int>>() {}.type
            gson.fromJson<List<Int>>(it, type)
        } ?: emptyList()
    }
}