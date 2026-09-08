package com.example.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class AttendanceRepository(private val attendanceDao: AttendanceDao) {
    val allRecords: Flow<List<AttendanceRecord>> = attendanceDao.getAllRecords()
    val allNotifications: Flow<List<AppNotification>> = attendanceDao.getAllNotifications()

    fun getRecordsInMonth(year: Int, month: Int): Flow<List<AttendanceRecord>> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        val startTime = calendar.timeInMillis
        
        calendar.set(year, month, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        val endTime = calendar.timeInMillis
        
        return attendanceDao.getRecordsInTimeRange(startTime, endTime)
    }

    suspend fun insertRecord(record: AttendanceRecord): Long {
        val resultId = attendanceDao.insertRecord(record)
        
        // Auto generate a log notification for this user action
        val activityType = if (record.type == "CHECK_IN") "Check-In" else "Check-Out"
        insertNotification(
            AppNotification(
                title = "$activityType Berhasil",
                message = "Anda telah $activityType pada pukul ${formatTime(record.timestamp)} di ${record.locationName}.",
                type = "SYSTEM"
            )
        )
        return resultId
    }

    suspend fun insertNotification(notification: AppNotification): Long {
        return attendanceDao.insertNotification(notification)
    }

    suspend fun markAllNotificationsAsRead() {
        attendanceDao.markAllNotificationsAsRead()
    }

    suspend fun clearAll() {
        attendanceDao.clearAllRecords()
        attendanceDao.clearAllNotifications()
    }

    suspend fun syncWithCentralServer(): Result<Int> {
        return try {
            val unsynced = attendanceDao.getUnsyncedRecords()
            if (unsynced.isEmpty()) {
                return Result.success(0)
            }
            
            // Simulating a secure API call to the central database
            kotlinx.coroutines.delay(1500) // simulation delay
            
            val currentTime = System.currentTimeMillis()
            attendanceDao.markAllAsSynced(currentTime)
            
            // Generate a sync notification
            insertNotification(
                AppNotification(
                    title = "Sinkronisasi Awan Selesai",
                    message = "Berhasil mensinkronisasi ${unsynced.size} data absensi ke sistem pusat.",
                    type = "SYNC"
                )
            )
            
            Result.success(unsynced.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatTime(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return sdf.format(date)
    }
}
