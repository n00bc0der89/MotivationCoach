package com.example.historymotivationcoach.data

import androidx.room.TypeConverter
import com.example.historymotivationcoach.data.entity.DeliveryStatus
import com.example.historymotivationcoach.data.entity.ScheduleMode
import java.time.DayOfWeek

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }
    
    @TypeConverter
    fun fromScheduleMode(value: ScheduleMode): String {
        return value.name
    }
    
    @TypeConverter
    fun toScheduleMode(value: String): ScheduleMode {
        return ScheduleMode.valueOf(value)
    }
    
    @TypeConverter
    fun fromDeliveryStatus(value: DeliveryStatus): String {
        return value.name
    }
    
    @TypeConverter
    fun toDeliveryStatus(value: String): DeliveryStatus {
        return DeliveryStatus.valueOf(value)
    }
    
    @TypeConverter
    fun fromDayOfWeekSet(value: Set<DayOfWeek>): String {
        return value.joinToString(",") { it.name }
    }
    
    @TypeConverter
    fun toDayOfWeekSet(value: String): Set<DayOfWeek> {
        return if (value.isEmpty()) emptySet() 
        else value.split(",").map { DayOfWeek.valueOf(it) }.toSet()
    }
}
