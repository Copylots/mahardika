package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: String,
    val employeeName: String,
    val timestamp: Long,
    val type: String, // "CHECK_IN", "CHECK_OUT"
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val isFaceVerified: Boolean,
    val faceMatchConfidence: Float,
    val isSynced: Boolean = false,
    val syncTimestamp: Long? = null,
    val photoPath: String? = null,
    val workShift: String = "Shift 1"
)

@Entity(tableName = "app_notifications")
data class AppNotification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String // "SYNC", "REMINDER", "SECURITY", "SYSTEM"
)
