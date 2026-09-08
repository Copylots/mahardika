package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getRecordsInTimeRange(startTime: Long, endTime: Long): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord): Long

    @Update
    suspend fun updateRecord(record: AttendanceRecord)

    @Query("UPDATE attendance_records SET isSynced = 1, syncTimestamp = :syncTime WHERE isSynced = 0")
    suspend fun markAllAsSynced(syncTime: Long)

    @Query("SELECT * FROM attendance_records WHERE isSynced = 0")
    suspend fun getUnsyncedRecords(): List<AttendanceRecord>

    @Query("DELETE FROM attendance_records")
    suspend fun clearAllRecords()

    // Notification Queries
    @Query("SELECT * FROM app_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<AppNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification): Long

    @Query("UPDATE app_notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllNotificationsAsRead()

    @Query("DELETE FROM app_notifications")
    suspend fun clearAllNotifications()
}
