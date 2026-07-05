package com.example

import android.Manifest
import android.app.Application
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.ClassroomRepository
import com.example.ui.components.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.*
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: QuietMonsterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup offline local database & repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ClassroomRepository(database.classroomDao())

        // ViewModel Factory
        val factory = QuietMonsterViewModelFactory(application, repository)
        viewModel = ViewModelProvider(this, factory)[QuietMonsterViewModel::class.java]

        setContent {
            MyApplicationTheme {
                MainScreen(viewModel)
            }
        }
    }
}

enum class NavigationTab {
    CLASSROOM, DASHBOARD, SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: QuietMonsterViewModel) {
    var selectedTab by remember { mutableStateOf(NavigationTab.CLASSROOM) }
    val context = LocalContext.current

    // Observe StateFlow values
    val isRecording by viewModel.isRecording.collectAsState()
    val isSimulatorMode by viewModel.isSimulatorMode.collectAsState()
    val currentNoise by viewModel.currentNoiseLevel.collectAsState()
    val threshold by viewModel.noiseThreshold.collectAsState()
    val selectedMonster by viewModel.selectedMonster.collectAsState()
    val mood by viewModel.monsterMood.collectAsState()
    val timerState by viewModel.timerState.collectAsState()
    val timeRemaining by viewModel.timeRemainingSeconds.collectAsState()
    val resetsCount by viewModel.resetsCount.collectAsState()
    val wakeUpAlert by viewModel.wakeUpAlert.collectAsState()

    // For historical logs in charts
    val allSessionLogs by viewModel.allSessionLogs.collectAsState()
    val allNoiseLogs by viewModel.allNoiseLogs.collectAsState()

    // Dialog State
    var showExportDialog by remember { mutableStateOf(false) }
    var exportText by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Floating alert for wake-up resets
    LaunchedEffect(wakeUpAlert) {
        if (wakeUpAlert) {
            Toast.makeText(context, "Ssshh! Mio woke up! The timer reset!", Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.CLASSROOM,
                    onClick = { selectedTab = NavigationTab.CLASSROOM },
                    icon = { Icon(Icons.Default.School, contentDescription = "Classroom", modifier = Modifier.size(28.dp)) },
                    label = { Text("Classroom", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_classroom")
                )
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.DASHBOARD,
                    onClick = { selectedTab = NavigationTab.DASHBOARD },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Dashboard", modifier = Modifier.size(28.dp)) },
                    label = { Text("Notebook", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_dashboard")
                )
                NavigationBarItem(
                    selected = selectedTab == NavigationTab.SETTINGS,
                    onClick = { selectedTab = NavigationTab.SETTINGS },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(28.dp)) },
                    label = { Text("Settings", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (selectedTab) {
                NavigationTab.CLASSROOM -> ClassroomScreen(
                    viewModel = viewModel,
                    isRecording = isRecording,
                    isSimulatorMode = isSimulatorMode,
                    currentNoise = currentNoise,
                    threshold = threshold,
                    selectedMonster = selectedMonster,
                    mood = mood,
                    timerState = timerState,
                    timeRemaining = timeRemaining,
                    resetsCount = resetsCount,
                    wakeUpAlert = wakeUpAlert
                )
                NavigationTab.DASHBOARD -> DashboardScreen(
                    sessionLogs = allSessionLogs,
                    noiseLogs = allNoiseLogs,
                    onClearData = { showClearConfirmDialog = true }
                )
                NavigationTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    selectedMonster = selectedMonster,
                    onExportData = {
                        exportText = viewModel.exportLocalData()
                        showExportDialog = true
                    }
                )
            }
        }
    }

    // Export local data dialog
    if (showExportDialog) {
        ExportDialog(
            exportText = exportText,
            onDismiss = { showExportDialog = false }
        )
    }

    // Clear confirmation dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Reset Teacher Notebook?") },
            text = { Text("This will permanently delete all local history logs, quiet sessions, and statistics. This action is irreversible.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllClassroomData()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "Classroom database cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_btn")
                ) {
                    Text("Delete All Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("clear_confirm_dialog")
        )
    }
}

@Composable
fun ClassroomScreen(
    viewModel: QuietMonsterViewModel,
    isRecording: Boolean,
    isSimulatorMode: Boolean,
    currentNoise: Float,
    threshold: Float,
    selectedMonster: MonsterCharacter,
    mood: MonsterMood,
    timerState: TimerState,
    timeRemaining: Int,
    resetsCount: Int,
    wakeUpAlert: Boolean
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.requestAudioPermissionAndRestart()
        } else {
            Toast.makeText(
                context,
                "Microphone permission denied. Running in simulator demonstration mode.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Background color shifts on wake up alert
    val screenBgColor by animateColorAsState(
        targetValue = if (wakeUpAlert) Color(0xFFFDE8E8) else MaterialTheme.colorScheme.background,
        animationSpec = tween(300),
        label = "bg_color"
    )

    val isRunningOrPaused = timerState == TimerState.RUNNING || timerState == TimerState.PAUSED
    val isIdle = timerState == TimerState.IDLE || timerState == TimerState.COMPLETED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBgColor)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val monsterSize by animateDpAsState(
            targetValue = if (isRunningOrPaused) 360.dp else 280.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "monster_size"
        )

        MonsterWidget(
            character = selectedMonster,
            mood = mood,
            currentNoise = currentNoise,
            threshold = threshold,
            modifier = Modifier.size(monsterSize).padding(bottom = 24.dp),
            onTap = { viewModel.tapMonsterSpike() }
        )

        AnimatedVisibility(
            visible = isIdle,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Microphone Permission / Fallback Indicator Badge
                if (isSimulatorMode) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clickable {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                            .testTag("simulator_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Demo Mode",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Simulator Active - Tap to Grant Mic Permission",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Microphone Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sound Meter Level & Threshold Slider Card
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Live Sound: ${currentNoise.toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Limit: ${threshold.toInt()}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red.copy(alpha = 0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Real-time horizontal visual slider
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            // Filled progress
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(currentNoise / 100f)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(
                                        if (currentNoise >= threshold) Color.Red
                                        else if (currentNoise >= threshold * 0.75f) Color(0xFFFFB300)
                                        else MaterialTheme.colorScheme.primary
                                    )
                            )

                            // Threshold limit line indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(3.dp)
                                    .align(Alignment.CenterStart)
                                    .offset(x = (320 * (threshold / 100f)).dp) // approximate visual offset mapping
                                    .background(Color.Red)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Slide to Adjust Noise Sensitivity Limit",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Slider(
                            value = threshold,
                            onValueChange = { viewModel.setNoiseThreshold(it) },
                            valueRange = 10f..95f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sensitivity_slider")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Silence Duration Selector Card
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Set Quiet Time Duration",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val duration by viewModel.sessionDurationMinutes.collectAsState()
                        
                        Text(
                            text = "${duration} Minutes",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        
                        Slider(
                            value = duration.toFloat(),
                            onValueChange = { viewModel.setSessionDuration(it.toInt()) },
                            valueRange = 1f..60f,
                            steps = 58,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("duration_slider")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Timer control buttons (Idle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.startSession() },
                        modifier = Modifier
                            .height(64.dp)
                            .width(200.dp)
                            .testTag("start_btn"),
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Start Quiet Time", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    if (timerState == TimerState.COMPLETED) {
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(
                            onClick = { viewModel.resetTimerStateToIdle() },
                            modifier = Modifier
                                .size(64.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .testTag("reset_btn")
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reset State",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isRunningOrPaused,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val mins = timeRemaining / 60
                val secs = timeRemaining % 60
                val formattedTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", mins, secs)

                Text(
                    text = formattedTime,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = when (timerState) {
                        TimerState.RUNNING -> MaterialTheme.colorScheme.primary
                        TimerState.PAUSED -> Color(0xFFFFB300)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (resetsCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Resets",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Timer Resets: $resetsCount",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Red
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(24.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (timerState == TimerState.RUNNING) viewModel.pauseSession()
                            else viewModel.resumeSession()
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .testTag(if (timerState == TimerState.RUNNING) "pause_btn" else "resume_btn")
                    ) {
                        Icon(
                            if (timerState == TimerState.RUNNING) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (timerState == TimerState.RUNNING) "Pause" else "Resume",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(32.dp))

                    IconButton(
                        onClick = { viewModel.cancelSession() },
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                            .testTag("stop_btn")
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    sessionLogs: List<com.example.data.SessionLog>,
    noiseLogs: List<com.example.data.NoiseLog>,
    onClearData: () -> Unit
) {
    // Math statistics
    val totalSessions = sessionLogs.size
    val perfectSessions = sessionLogs.count { it.successful }
    val successRate = if (totalSessions > 0) (perfectSessions.toFloat() / totalSessions * 100).toInt() else 0
    val totalResets = sessionLogs.sumOf { it.resetsCount }
    val avgNoise = if (sessionLogs.isNotEmpty()) sessionLogs.map { it.averageNoise }.average().toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Teacher Notebook",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Track improvements over the semester",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (sessionLogs.isNotEmpty()) {
                IconButton(
                    onClick = onClearData,
                    modifier = Modifier.size(48.dp).testTag("clear_history_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear History",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // High level stats widgets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Perfect Sessions",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "$perfectSessions / $totalSessions",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Quiet Score",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "$successRate%",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total Resets",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$totalResets",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(90.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Average Volume",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$avgNoise%",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Visual charts
        NoiseLineChart(
            logs = noiseLogs,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        WeeklyTrendsChart(
            sessions = sessionLogs,
            modifier = Modifier.padding(vertical = 8.dp)
        )
    }
}

@Composable
fun SettingsScreen(
    viewModel: QuietMonsterViewModel,
    selectedMonster: MonsterCharacter,
    onExportData: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Classroom Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Configure Quiet Monster classroom parameters",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Parameter 1: Choose character
        Text(
            text = "Select Quiet Monster Mascot",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chunkedChars = MonsterCharacter.values().toList().chunked(3)
            chunkedChars.forEach { rowChars ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowChars.forEach { char ->
                        val isSelected = char == selectedMonster
                        val borderBrush = if (isSelected) {
                            Brush.sweepGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                        } else {
                            null
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setMonsterCharacter(char) }
                                .then(
                                    if (borderBrush != null) Modifier.border(
                                        2.dp,
                                        borderBrush,
                                        RoundedCornerShape(12.dp)
                                    ) else Modifier
                                )
                                .testTag("monster_select_${char.name.lowercase()}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            Color(android.graphics.Color.parseColor(char.primaryColorHex)),
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = char.displayName.split(" ")[0], // just first name
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = char.snores.split(" ")[0],
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (rowChars.size < 3) {
                        repeat(3 - rowChars.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // Parameter: Sound selection for the selected monster
        val currentMonsterSound = viewModel.monsterSounds.collectAsState().value[selectedMonster] ?: MonsterSound.ROAR
        Text(
            text = "Wake-Up Sound for ${selectedMonster.displayName}",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chunkedSounds = MonsterSound.values().toList().chunked(3)
            chunkedSounds.forEach { rowSounds ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowSounds.forEach { sound ->
                        val isSelected = sound == currentMonsterSound
                        val borderBrush = if (isSelected) {
                            Brush.sweepGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))
                        } else {
                            null
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setMonsterSound(selectedMonster, sound) }
                                .then(
                                    if (borderBrush != null) Modifier.border(
                                        2.dp,
                                        borderBrush,
                                        RoundedCornerShape(12.dp)
                                    ) else Modifier
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = sound.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    if (rowSounds.size < 3) {
                        repeat(3 - rowSounds.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Parameter 2: Local privacy actions
        Text(
            text = "Privacy & Local Sharing Tools",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Button(
            onClick = onExportData,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("export_data_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = "Export")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Export Logs Offline", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Privacy Checkmark",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "No metrics go out of the device. Student identities are never captured or processed.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        
        // Parameter 3: Experimental Features
        Text(
            text = "Experimental Features",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        val isBackgroundListening = viewModel.backgroundListeningEnabled.collectAsState().value
        val isFloatingBubble = viewModel.floatingBubbleEnabled.collectAsState().value
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Background Listening Mode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Allow the app to continue monitoring noise levels even when you switch to other apps.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isBackgroundListening,
                        onCheckedChange = { viewModel.toggleBackgroundListening(it) },
                        modifier = Modifier.testTag("toggle_background_listening")
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Floating Bubble Mode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Show the Quiet Monster as a floating bubble over other apps when minimized.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isFloatingBubble,
                        onCheckedChange = { viewModel.toggleFloatingBubble(it) },
                        modifier = Modifier.testTag("toggle_floating_bubble")
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
