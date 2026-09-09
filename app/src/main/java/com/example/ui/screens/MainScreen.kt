package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.AttendanceViewModel
import com.example.ui.SyncState

sealed class NavigationScreen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : NavigationScreen("dashboard", "Dashboard", Icons.Default.Dashboard)
    object Presensi : NavigationScreen("presensi", "Presensi", Icons.Default.CameraFront)
    object Laporan : NavigationScreen("laporan", "Laporan", Icons.Default.Assessment)
    object Pengaturan : NavigationScreen("pengaturan", "Pengaturan", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<NavigationScreen>(NavigationScreen.Dashboard) }
    
    val isTwoFactorEnabled by viewModel.isTwoFactorEnabled.collectAsState()
    val isTwoFactorAuthenticated by viewModel.isTwoFactorAuthenticated.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val records by viewModel.allRecords.collectAsState()
    
    val unsyncedCount = records.count { !it.isSynced }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 600.dp

        // Master 2FA Authentication Interstitial Block Layout
        if (isTwoFactorEnabled && !isTwoFactorAuthenticated) {
            TwoFactorGate(viewModel)
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Presensi Karyawan PT. MSS",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Quick security active tag
                                if (isTwoFactorEnabled) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = "Secured",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        actions = {
                            // Sync Status Icon Command
                            IconButton(
                                onClick = { viewModel.syncData() },
                                modifier = Modifier.testTag("top_cloud_sync_icon")
                            ) {
                                when (syncState) {
                                    is SyncState.Syncing -> {
                                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                    }
                                    is SyncState.Success -> {
                                        Icon(Icons.Outlined.CloudDone, contentDescription = "Synced Successfully", tint = Color(0xFF4CAF50))
                                    }
                                    is SyncState.Error -> {
                                        Icon(Icons.Outlined.CloudOff, contentDescription = "Sync Error", tint = MaterialTheme.colorScheme.error)
                                    }
                                    else -> {
                                        if (unsyncedCount > 0) {
                                            BadgedBox(
                                                badge = { Badge { Text(unsyncedCount.toString()) } }
                                            ) {
                                                Icon(Icons.Outlined.CloudQueue, contentDescription = "Unsynced Data Pending")
                                            }
                                        } else {
                                            Icon(Icons.Outlined.CloudDone, contentDescription = "Synced")
                                        }
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                        )
                    )
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                        ) {
                            val items = listOf(
                                NavigationScreen.Dashboard,
                                NavigationScreen.Presensi,
                                NavigationScreen.Laporan,
                                NavigationScreen.Pengaturan
                            )
                            items.forEach { item ->
                                val selected = currentScreen == item
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentScreen = item },
                                    icon = { Icon(item.icon, contentDescription = item.title) },
                                    label = { Text(item.title) },
                                    modifier = Modifier.testTag("nav_tab_${item.route}")
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // Display Side Navigation Rail for Large Tablets/Desktop layout
                    if (isWideScreen) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                            header = {
                                Spacer(modifier = Modifier.height(16.dp))
                                FloatingActionButton(
                                    onClick = { viewModel.syncData() },
                                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp),
                                    modifier = Modifier.testTag("rail_sync_fab")
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = "Sync Now")
                                }
                            }
                        ) {
                            Spacer(modifier = Modifier.weight(1.0f))
                            val items = listOf(
                                NavigationScreen.Dashboard,
                                NavigationScreen.Presensi,
                                NavigationScreen.Laporan,
                                NavigationScreen.Pengaturan
                            )
                            items.forEach { item ->
                                val selected = currentScreen == item
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentScreen = item },
                                    icon = { Icon(item.icon, contentDescription = item.title) },
                                    label = { Text(item.title) },
                                    modifier = Modifier.testTag("nav_rail_${item.route}")
                                )
                            }
                            Spacer(modifier = Modifier.weight(1.0f))
                        }
                    }

                    // Master Content Display
                    Box(modifier = Modifier.weight(1.0f)) {
                        when (currentScreen) {
                            is NavigationScreen.Dashboard -> DashboardScreen(viewModel)
                            is NavigationScreen.Presensi -> CheckInOutScreen(viewModel)
                            is NavigationScreen.Laporan -> HistoryScreen(viewModel)
                            is NavigationScreen.Pengaturan -> SettingsScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoFactorGate(viewModel: AttendanceViewModel) {
    var code by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secured Lock",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Akses Terkunci",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Akun Anda dilindungi Otentikasi Dua Faktor (2FA). Silakan masukkan kode OTP dari Google Authenticator Anda atau masukkan demo code '123456'.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { 
                        code = it
                        codeError = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("gate_otp_input"),
                    label = { Text("Kode OTP 6-Digit") },
                    isError = codeError,
                    supportingText = {
                        if (codeError) {
                            Text("Kode verifikasi salah! Coba demo code '123456'.")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val authenticated = viewModel.verifyTwoFactorCode(code)
                        if (!authenticated) {
                            codeError = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_gate_otp_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buka Kunci Akses", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = { viewModel.toggleTwoFactor(false) }
                ) {
                    Text("Nonaktifkan Pengaman 2FA (Darurat)")
                }
            }
        }
    }
}
