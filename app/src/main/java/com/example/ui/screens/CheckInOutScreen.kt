package com.example.ui.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.AttendanceViewModel
import com.example.ui.FaceState
import com.example.ui.LocationState
import java.util.Locale
import java.util.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInOutScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    val locationState by viewModel.currentLocation.collectAsState()
    val faceState by viewModel.faceVerificationState.collectAsState()
    
    var selectedType by remember { mutableStateOf("CHECK_IN") }
    var selectedShift by remember { mutableStateOf("Shift 1") }

    // Permissions Handling
    var hasLocationPermission by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: false
        
        if (hasLocationPermission) {
            viewModel.startLocationUpdates()
        }
    }

    LaunchedEffect(Unit) {
        val fineLocationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val cameraGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        hasLocationPermission = fineLocationGranted
        hasCameraPermission = cameraGranted

        if (!fineLocationGranted || !cameraGranted) {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CAMERA
                )
            )
        } else {
            viewModel.startLocationUpdates()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isWideScreen = maxWidth > 600.dp
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Presensi Presisi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Lakukan perekaman absensi kerja harian Anda menggunakan verifikasi lokasi GPS dan otentikasi biometrik wajah secara real-time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Dual Column Responsive Box for Tablet/Wide Devices
            if (isWideScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1000.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        TypeSelectionCard(selectedType) { selectedType = it }
                        Spacer(modifier = Modifier.height(16.dp))
                        ShiftSelectionCard(selectedShift) { selectedShift = it }
                        Spacer(modifier = Modifier.height(16.dp))
                        GPSLocationCard(locationState, hasLocationPermission) {
                            if (hasLocationPermission) viewModel.startLocationUpdates() else permissionsLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        FaceScanCard(faceState, hasCameraPermission, viewModel)
                    }
                }
            } else {
                // Mobile Stack
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TypeSelectionCard(selectedType) { selectedType = it }
                    ShiftSelectionCard(selectedShift) { selectedShift = it }
                    GPSLocationCard(locationState, hasLocationPermission) {
                        if (hasLocationPermission) viewModel.startLocationUpdates() else permissionsLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                    }
                    FaceScanCard(faceState, hasCameraPermission, viewModel)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Submission Button
            val isReady = locationState is LocationState.Success && faceState is FaceState.Success
            Button(
                onClick = { viewModel.submitAttendance(selectedType, selectedShift) },
                enabled = isReady,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 400.dp)
                    .height(56.dp)
                    .testTag("submit_attendance_btn"),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = if (selectedType == "CHECK_IN") "Kirim Check-In Kerja" else "Kirim Check-Out Kerja",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun TypeSelectionCard(
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val checkInSelected = selectedType == "CHECK_IN"
            Button(
                onClick = { onTypeSelected("CHECK_IN") },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("type_check_in_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (checkInSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (checkInSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Check-In", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { onTypeSelected("CHECK_OUT") },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("type_check_out_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!checkInSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (!checkInSelected) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Check-Out", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GPSLocationCard(
    state: LocationState,
    hasPermission: Boolean,
    onReloadLocation: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lokasi Kehadiran (GPS)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onReloadLocation) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh GPS")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (state) {
                is LocationState.Idle -> {
                    StatusRow(Icons.Default.LocationDisabled, "Mencari sinyal satelit GPS...", MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is LocationState.Locating -> {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Mengunci koordinat GPS...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is LocationState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Satelit Terkunci (Presisi Tinggi)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Gedung Kantor Pusat, Lantai 12, Sudirman, Jakarta Selatan",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Lintang: ${state.latitude} | Bujur: ${state.longitude}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is LocationState.Error -> {
                    StatusRow(Icons.Default.Error, state.message, MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun FaceScanCard(
    state: FaceState,
    hasPermission: Boolean,
    viewModel: AttendanceViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Verifikasi Wajah Biometrik",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Camera Area / Preview Panel
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .border(4.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (hasPermission && state is FaceState.Scanning) {
                    CameraPreview()
                    
                    // Scanning Overlay glowing line
                    val infiniteTransition = rememberInfiniteTransition(label = "scan")
                    val translateY by infiniteTransition.animateFloat(
                        initialValue = -100f,
                        targetValue = 100f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "line"
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .offset(y = translateY.dp)
                            .background(Color(0xFF00FFCC))
                    )
                } else if (hasPermission && state is FaceState.Success) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(72.dp)
                        )
                    }
                } else {
                    // Placeholder Icon Graphic
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        modifier = Modifier.size(120.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Face Scan Info & Trigger Action
            when (state) {
                is FaceState.Idle -> {
                    Text(
                        text = "Arahkan kamera ke wajah Anda",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.startFaceVerification() },
                        enabled = hasPermission,
                        modifier = Modifier.testTag("start_scan_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Mulai Pemindaian")
                    }
                }
                is FaceState.Scanning -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Menganalisis kemiripan fitur wajah... (${(state.progress * 100).toInt()}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is FaceState.Success -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color(0xFFE8F5E9).copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Wajah Berhasil Terotentikasi!",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tingkat Kecocokan: ${String.format(Locale.getDefault(), "%.2f", state.confidence)}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF1B5E20)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.resetFaceVerification() }) {
                            Text("Ulangi Pindai")
                        }
                    }
                }
                is FaceState.Error -> {
                    StatusRow(Icons.Default.Error, state.message, MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun CameraPreview() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    // Silent fail for mock safety
                }
            }, executor)
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun StatusRow(icon: ImageVector, text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
    }
}

@Composable
fun ShiftSelectionCard(
    selectedShift: String,
    onShiftSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Pilih Shift Kerja",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val shifts = listOf(
                    "Shift 1" to "07:00 - 19:00",
                    "Shift 2" to "19:00 - 07:00"
                )
                shifts.forEach { (shiftName, timeWindow) ->
                    val isSelected = selectedShift == shiftName
                    OutlinedButton(
                        onClick = { onShiftSelected(shiftName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("shift_btn_${shiftName.replace(" ", "_")}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        ),
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onShiftSelected(shiftName) }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = shiftName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                text = timeWindow,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }
}
