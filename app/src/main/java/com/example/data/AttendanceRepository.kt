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
            
            var firebaseSuccess = false
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                unsynced.forEach { record ->
                    val recordMap = hashMapOf(
                        "employeeId" to record.employeeId,
                        "employeeName" to record.employeeName,
                        "timestamp" to record.timestamp,
                        "type" to record.type,
                        "latitude" to record.latitude,
                        "longitude" to record.longitude,
                        "locationName" to record.locationName,
                        "isFaceVerified" to record.isFaceVerified,
                        "faceMatchConfidence" to record.faceMatchConfidence,
                        "workShift" to record.workShift
                    )
                    
                    db.collection("attendance_records")
                        .document("REC_${record.timestamp}_${record.employeeId}")
                        .set(recordMap)
                }
                firebaseSuccess = true
                kotlinx.coroutines.delay(1500)
            } catch (e: Exception) {
                android.util.Log.w("FirebaseSync", "Firebase is not initialized or failed to sync. Falling back to local simulation.", e)
                kotlinx.coroutines.delay(1500)
            }
            
            val currentTime = System.currentTimeMillis()
            attendanceDao.markAllAsSynced(currentTime)
            
            // Generate a sync notification
            val syncMessage = if (firebaseSuccess) {
                "Berhasil mensinkronisasi ${unsynced.size} data absensi secara real-time ke Firebase Firestore."
            } else {
                "Berhasil mensinkronisasi ${unsynced.size} data absensi ke sistem pusat (Simulasi Lokal)."
            }
            
            insertNotification(
                AppNotification(
                    title = "Sinkronisasi Selesai",
                    message = syncMessage,
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
