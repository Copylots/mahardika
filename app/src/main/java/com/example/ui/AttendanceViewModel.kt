package com.example.ui

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppNotification
import com.example.data.AttendanceDatabase
import com.example.data.AttendanceRecord
import com.example.data.AttendanceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AttendanceRepository
    val allRecords: StateFlow<List<AttendanceRecord>>
    val allNotifications: StateFlow<List<AppNotification>>

    // UI States
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _isTwoFactorEnabled = MutableStateFlow(false)
    val isTwoFactorEnabled = _isTwoFactorEnabled.asStateFlow()

    private val _isTwoFactorAuthenticated = MutableStateFlow(false)
    val isTwoFactorAuthenticated = _isTwoFactorAuthenticated.asStateFlow()

    private val _currentLocation = MutableStateFlow<LocationState>(LocationState.Idle)
    val currentLocation = _currentLocation.asStateFlow()

    private val _faceVerificationState = MutableStateFlow<FaceState>(FaceState.Idle)
    val faceVerificationState = _faceVerificationState.asStateFlow()

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState = _syncState.asStateFlow()

    private val _isAdminMode = MutableStateFlow(false)
    val isAdminMode = _isAdminMode.asStateFlow()

    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus = _exportStatus.asStateFlow()

    init {
        val database = AttendanceDatabase.getDatabase(application)
        repository = AttendanceRepository(database.attendanceDao())
        
        allRecords = repository.allRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        
        allNotifications = repository.allNotifications.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Setup notification channel
        createNotificationChannel()
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun toggleTwoFactor(enabled: Boolean) {
        _isTwoFactorEnabled.value = enabled
        if (!enabled) {
            _isTwoFactorAuthenticated.value = false
        }
    }

    fun verifyTwoFactorCode(code: String): Boolean {
        // Simple mock OTP validation (e.g., matching any 6-digit code or specific demo '123456')
        return if (code == "123456" || code.length == 6) {
            _isTwoFactorAuthenticated.value = true
            viewModelScope.launch {
                repository.insertNotification(
                    AppNotification(
                        title = "Keamanan: 2FA Terverifikasi",
                        message = "Otentikasi dua faktor berhasil diselesaikan.",
                        type = "SECURITY"
                    )
                )
            }
            true
        } else {
            false
        }
    }

    fun resetTwoFactorAuth() {
        if (_isTwoFactorEnabled.value) {
            _isTwoFactorAuthenticated.value = false
        }
    }

    fun setAdminMode(isAdmin: Boolean) {
        _isAdminMode.value = isAdmin
    }

    // GPS Location Simulation and Integration
    fun updateLocation(latitude: Double, longitude: Double, provider: String = "GPS Mocked") {
        viewModelScope.launch {
            _currentLocation.value = LocationState.Success(
                latitude = latitude,
                longitude = longitude,
                address = getSimulatedAddress(latitude, longitude)
            )
        }
    }

    fun startLocationUpdates() {
        _currentLocation.value = LocationState.Locating
        // Simulate GPS lock with standard office coordinate center
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            // Latitude & Longitude close to Sudirman, Jakarta
            updateLocation(-6.2198, 106.8208, "GPS")
        }
    }

    private fun getSimulatedAddress(lat: Double, lng: Double): String {
        return "Gedung Kantor Pusat, Lantai 12, Sudirman, Jakarta Selatan (-6.2198, 106.8208)"
    }

    // Camera and Face Scan Simulation
    fun startFaceVerification() {
        _faceVerificationState.value = FaceState.Scanning(0f)
        viewModelScope.launch {
            for (progress in 1..10) {
                kotlinx.coroutines.delay(200)
                _faceVerificationState.value = FaceState.Scanning(progress / 10f)
            }
            val confidence = 95.0f + Random().nextFloat() * 4.9f // 95% - 99.9%
            _faceVerificationState.value = FaceState.Success(confidence)
        }
    }

    fun resetFaceVerification() {
        _faceVerificationState.value = FaceState.Idle
    }

    // Attendance Actions
    fun submitAttendance(type: String, workShift: String) {
        val loc = _currentLocation.value
        val face = _faceVerificationState.value
        
        if (loc !is LocationState.Success || face !is FaceState.Success) return

        viewModelScope.launch {
            val record = AttendanceRecord(
                employeeId = "EMP-92841",
                employeeName = "Budi Santoso",
                timestamp = System.currentTimeMillis(),
                type = type,
                latitude = loc.latitude,
                longitude = loc.longitude,
                locationName = loc.address,
                isFaceVerified = true,
                faceMatchConfidence = face.confidence,
                workShift = workShift
            )
            repository.insertRecord(record)
            
            // Trigger actual Local push notification
            showLocalPushNotification(
                title = "Presensi Karyawan PT. MSS",
                message = "Berhasil ${if (type == "CHECK_IN") "Check-In" else "Check-Out"} ($workShift) di ${loc.address}!"
            )
            
            // Reset states for next actions
            _faceVerificationState.value = FaceState.Idle
        }
    }

    // Sync Actions
    fun syncData() {
        if (_syncState.value == SyncState.Syncing) return
        _syncState.value = SyncState.Syncing
        viewModelScope.launch {
            val result = repository.syncWithCentralServer()
            result.onSuccess { count ->
                _syncState.value = SyncState.Success(count)
                kotlinx.coroutines.delay(3000)
                _syncState.value = SyncState.Idle
            }.onFailure {
                _syncState.value = SyncState.Error("Koneksi gagal. Coba lagi nanti.")
                kotlinx.coroutines.delay(3000)
                _syncState.value = SyncState.Idle
            }
        }
    }

    // Export Actions (PDF/Excel)
    fun exportData(format: String) {
        viewModelScope.launch {
            _exportStatus.value = "Mengekspor data ke format $format..."
            kotlinx.coroutines.delay(1500)
            
            val records = allRecords.value
            val context = getApplication<Application>()
            
            val fileName = "Laporan_Absensi_${System.currentTimeMillis()}.$format"
            val state = Environment.getExternalStorageState()
            
            val file = if (Environment.MEDIA_MOUNTED == state) {
                File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
            } else {
                File(context.filesDir, fileName)
            }

            try {
                FileOutputStream(file).use { out ->
                    if (format.lowercase() == "xlsx" || format.lowercase() == "xls" || format.lowercase() == "csv") {
                        // Generate beautifully formatted CSV which opens directly as Excel
                        val builder = StringBuilder()
                        builder.append("No,ID Karyawan,Nama Karyawan,Waktu,Tipe Presensi,Lintang,Bujur,Lokasi,Verifikasi Wajah,Kecocokan Face (%)\n")
                        records.forEachIndexed { index, record ->
                            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(record.timestamp))
                            builder.append("${index + 1},${record.employeeId},${record.employeeName},$dateStr,${record.type},${record.latitude},${record.longitude},\"${record.locationName}\",${record.isFaceVerified},${String.format(Locale.US, "%.1f", record.faceMatchConfidence)}\n")
                        }
                        out.write(builder.toString().toByteArray())
                    } else {
                        // Generate rich mock PDF format representation text
                        val builder = StringBuilder()
                        builder.append("===================================================================\n")
                        builder.append("                 LAPORAN PRESENSI KEHADIRAN KARYAWAN                \n")
                        builder.append("===================================================================\n")
                        builder.append("Dicetak Pada: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                        builder.append("Total Rekam Data: ${records.size}\n\n")
                        builder.append(String.format("%-4s %-12s %-15s %-20s %-10s %-15s\n", "No", "ID", "Nama", "Tanggal/Waktu", "Tipe", "Status Wajah"))
                        builder.append("-------------------------------------------------------------------\n")
                        records.forEachIndexed { index, record ->
                            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(record.timestamp))
                            builder.append(String.format("%-4d %-12s %-15s %-20s %-10s %-15s\n", 
                                index + 1, 
                                record.employeeId, 
                                record.employeeName, 
                                dateStr, 
                                record.type, 
                                if (record.isFaceVerified) "Terverifikasi (${String.format(Locale.US, "%.1f", record.faceMatchConfidence)}%)" else "Gagal"
                            ))
                        }
                        builder.append("===================================================================\n")
                        out.write(builder.toString().toByteArray())
                    }
                }
                _exportStatus.value = "Berhasil diekspor! File disimpan di: ${file.name}"
                repository.insertNotification(
                    AppNotification(
                        title = "Ekspor Laporan Selesai",
                        message = "Dokumen $fileName berhasil diekspor ke penyimpanan internal.",
                        type = "SYSTEM"
                    )
                )
            } catch (e: Exception) {
                _exportStatus.value = "Gagal mengekspor: ${e.message}"
            }
            
            kotlinx.coroutines.delay(4000)
            _exportStatus.value = null
        }
    }

    // Local Notification Management
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Presensi Reminder"
            val descriptionText = "Jadwal Pengingat Presensi Masuk & Keluar Kerja"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("PRESENSI_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showLocalPushNotification(title: String, message: String) {
        val context = getApplication<Application>()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val builder = NotificationCompat.Builder(context, "PRESENSI_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            
        notificationManager.notify(Random().nextInt(), builder.build())
    }

    fun triggerScheduledReminderSimulation(type: String) {
        viewModelScope.launch {
            val msg = if (type == "CHECK_IN") {
                "Pengingat: Waktunya melakukan Check-In masuk kerja pagi ini! Pastikan GPS & Kamera aktif."
            } else {
                "Pengingat: Jangan lupa melakukan Check-Out pulang kerja hari ini. Selamat beristirahat!"
            }
            
            showLocalPushNotification("Jadwal Presensi", msg)
            
            repository.insertNotification(
                AppNotification(
                    title = "Alarm Pengingat Terkirim",
                    message = "Mengirimkan notifikasi push pengingat jadwal $type.",
                    type = "REMINDER"
                )
            )
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }
}

// State Definitions
sealed class LocationState {
    object Idle : LocationState()
    object Locating : LocationState()
    data class Success(val latitude: Double, val longitude: Double, val address: String) : LocationState()
    data class Error(val message: String) : LocationState()
}

sealed class FaceState {
    object Idle : FaceState()
    data class Scanning(val progress: Float) : FaceState()
    data class Success(val confidence: Float) : FaceState()
    data class Error(val message: String) : FaceState()
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val syncedCount: Int) : SyncState()
    data class Error(val message: String) : SyncState()
}
