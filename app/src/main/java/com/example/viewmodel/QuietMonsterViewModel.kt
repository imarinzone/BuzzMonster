package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioMonitor
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class TimerState {
    IDLE, RUNNING, PAUSED, COMPLETED
}

enum class MonsterMood {
    SLEEPING,    // Super quiet, cute snoozing bubbles
    WARY,        // Sound getting close to threshold, eyes open a crack, looking around
    AWAKE        // Too loud! Wakes up surprised/screaming, timer resets!
}

enum class MonsterCharacter(val displayName: String, val primaryColorHex: String, val snores: String) {
    MIO("Mio the Blue Cub", "#42A5F5", "Zzz... snore..."),
    GLOOP("Gloop the Green Slime", "#66BB6A", "Bloop... glup..."),
    SPIKE("Spike the Orange Dino", "#FFA726", "Zzz... rawr..."),
    LUNA("Luna the Purple Owl", "#AB47BC", "Hoot... zzz..."),
    TRIXIE("Trixie the Pink Bunny", "#EC407A", "Sniff... zzz..."),
    IGNIS("Ignis the Fire Spark", "#EF5350", "Crackle... zzz...")
}

class QuietMonsterViewModel(
    application: Application,
    private val repository: ClassroomRepository
) : AndroidViewModel(application) {

    private val context = application.applicationContext
    val audioMonitor = AudioMonitor(context)

    // Configuration Settings
    private val _noiseThreshold = MutableStateFlow(40f) // 0 to 100
    val noiseThreshold = _noiseThreshold.asStateFlow()

    private val _sessionDurationMinutes = MutableStateFlow(2) // default 2 mins
    val sessionDurationMinutes = _sessionDurationMinutes.asStateFlow()

    private val _selectedMonster = MutableStateFlow(MonsterCharacter.MIO)
    val selectedMonster = _selectedMonster.asStateFlow()

    // Timer & Monitoring State
    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState = _timerState.asStateFlow()

    private val _timeRemainingSeconds = MutableStateFlow(120)
    val timeRemainingSeconds = _timeRemainingSeconds.asStateFlow()

    private val _monsterMood = MutableStateFlow(MonsterMood.SLEEPING)
    val monsterMood = _monsterMood.asStateFlow()

    // Session-specific logs
    private val _currentSessionId = MutableStateFlow<Long?>(null)
    private val _resetsCount = MutableStateFlow(0)
    val resetsCount = _resetsCount.asStateFlow()

    // Sound levels stream
    val currentNoiseLevel: StateFlow<Float> = audioMonitor.noiseLevel
    val isRecording: StateFlow<Boolean> = audioMonitor.isRecording
    val isSimulatorMode: StateFlow<Boolean> = audioMonitor.isSimulatorMode

    // Database flows
    val allSessionLogs: StateFlow<List<SessionLog>> = repository.allSessionLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNoiseLogs: StateFlow<List<NoiseLog>> = repository.allNoiseLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // For the active/recent session, keep local buffer of noise samples
    private val _recentNoiseSamples = MutableStateFlow<List<Float>>(emptyList())
    val recentNoiseSamples = _recentNoiseSamples.asStateFlow()

    // Cooldown state after a monster wakes up
    private val _wakeUpAlert = MutableStateFlow(false)
    val wakeUpAlert = _wakeUpAlert.asStateFlow()

    private var timerJob: Job? = null
    private var noiseSamplingJob: Job? = null

    // Date formatting helper
    private val dateFormat = SimpleDateFormat("yyyy-MM-DD", Locale.getDefault())

    init {
        // Load default settings from SharedPreferences
        val prefs = context.getSharedPreferences("quiet_monster_prefs", Context.MODE_PRIVATE)
        _noiseThreshold.value = prefs.getFloat("noise_threshold", 45f)
        _sessionDurationMinutes.value = prefs.getInt("session_duration_minutes", 2)
        val savedMonsterIndex = prefs.getInt("selected_monster_index", 0)
        _selectedMonster.value = MonsterCharacter.values().getOrElse(savedMonsterIndex) { MonsterCharacter.MIO }

        _timeRemainingSeconds.value = _sessionDurationMinutes.value * 60

        // Start background recording of noise level (untracked when session isn't running, but displays visual feedback)
        audioMonitor.startMonitoring()
    }

    fun setNoiseThreshold(value: Float) {
        _noiseThreshold.value = value
        val prefs = context.getSharedPreferences("quiet_monster_prefs", Context.MODE_PRIVATE)
        prefs.edit().putFloat("noise_threshold", value).apply()
    }

    fun setSessionDuration(minutes: Int) {
        _sessionDurationMinutes.value = minutes
        val prefs = context.getSharedPreferences("quiet_monster_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("session_duration_minutes", minutes).apply()
        if (_timerState.value == TimerState.IDLE) {
            _timeRemainingSeconds.value = minutes * 60
        }
    }

    fun setMonsterCharacter(monster: MonsterCharacter) {
        _selectedMonster.value = monster
        val prefs = context.getSharedPreferences("quiet_monster_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("selected_monster_index", monster.ordinal).apply()
    }

    fun requestAudioPermissionAndRestart() {
        // Stop currently running background monitor (which is probably in simulator fallback)
        audioMonitor.stopMonitoring()
        // Start monitoring again (will check if permission is now granted and use real microphone if so)
        audioMonitor.startMonitoring()
    }

    fun startSession() {
        if (_timerState.value == TimerState.RUNNING) return

        val sessionId = System.currentTimeMillis()
        _currentSessionId.value = sessionId
        _resetsCount.value = 0
        _timeRemainingSeconds.value = _sessionDurationMinutes.value * 60
        _timerState.value = TimerState.RUNNING
        _monsterMood.value = MonsterMood.SLEEPING
        _recentNoiseSamples.value = emptyList()
        _wakeUpAlert.value = false

        // Start timer countdown
        timerJob = viewModelScope.launch {
            while (_timeRemainingSeconds.value > 0) {
                delay(1000)
                if (_timerState.value == TimerState.RUNNING) {
                    _timeRemainingSeconds.value -= 1
                }
            }
            // Timer finished successfully!
            completeSession(true)
        }

        // Start noise sampling and threshold checking
        noiseSamplingJob = viewModelScope.launch {
            var sampleCount = 0
            while (_timerState.value == TimerState.RUNNING) {
                delay(200) // sample noise level 5 times a second
                val level = currentNoiseLevel.value
                val activeThreshold = _noiseThreshold.value

                // Record sample locally
                val updatedList = _recentNoiseSamples.value.toMutableList()
                if (updatedList.size > 50) updatedList.removeAt(0) // keep last 50 samples for immediate visual graph
                updatedList.add(level)
                _recentNoiseSamples.value = updatedList

                // Log periodically to DB (every 1 second / 5 samples)
                sampleCount++
                if (sampleCount >= 5) {
                    sampleCount = 0
                    repository.insertNoiseLog(
                        NoiseLog(
                            amplitude = level,
                            threshold = activeThreshold,
                            sessionId = sessionId
                        )
                    )
                }

                // Check noise threshold
                if (level >= activeThreshold) {
                    triggerNoiseWakeup()
                } else if (level >= activeThreshold * 0.75f) {
                    _monsterMood.value = MonsterMood.WARY
                } else {
                    _monsterMood.value = MonsterMood.SLEEPING
                }
            }
        }
    }

    private suspend fun triggerNoiseWakeup() {
        _resetsCount.value += 1
        _monsterMood.value = MonsterMood.AWAKE
        _wakeUpAlert.value = true

        // Reset timer back to full duration
        _timeRemainingSeconds.value = _sessionDurationMinutes.value * 60

        // Pause timer briefly so kids notice the monster is awake!
        _timerState.value = TimerState.PAUSED
        delay(3500) // 3.5 seconds of "awake/alarm" state for visual impact

        _wakeUpAlert.value = false
        if (_currentSessionId.value != null) {
            // Resume quiet monitoring
            _timerState.value = TimerState.RUNNING
            _monsterMood.value = MonsterMood.SLEEPING
        }
    }

    fun pauseSession() {
        if (_timerState.value == TimerState.RUNNING) {
            _timerState.value = TimerState.PAUSED
        }
    }

    fun resumeSession() {
        if (_timerState.value == TimerState.PAUSED) {
            _timerState.value = TimerState.RUNNING
            _wakeUpAlert.value = false
            _monsterMood.value = MonsterMood.SLEEPING
        }
    }

    fun cancelSession() {
        viewModelScope.launch {
            completeSession(false)
        }
    }

    private suspend fun completeSession(naturalFinish: Boolean) {
        timerJob?.cancel()
        timerJob = null
        noiseSamplingJob?.cancel()
        noiseSamplingJob = null

        val sessId = _currentSessionId.value
        if (sessId != null) {
            val samples = _recentNoiseSamples.value
            val maxVal = if (samples.isNotEmpty()) samples.maxOrNull() ?: 0f else 0f
            val avgVal = if (samples.isNotEmpty()) samples.average().toFloat() else 0f

            // Save session outcomes to SQLite database
            val successfulOutcome = naturalFinish && _resetsCount.value == 0
            repository.insertSessionLog(
                SessionLog(
                    id = sessId,
                    dateString = dateFormat.format(Date(sessId)),
                    durationSeconds = _sessionDurationMinutes.value * 60,
                    resetsCount = _resetsCount.value,
                    successful = successfulOutcome,
                    maxNoise = maxVal,
                    averageNoise = avgVal
                )
            )
        }

        _timerState.value = if (naturalFinish) TimerState.COMPLETED else TimerState.IDLE
        _monsterMood.value = if (naturalFinish) MonsterMood.SLEEPING else MonsterMood.SLEEPING
        _currentSessionId.value = null
    }

    fun resetTimerStateToIdle() {
        _timerState.value = TimerState.IDLE
        _timeRemainingSeconds.value = _sessionDurationMinutes.value * 60
        _resetsCount.value = 0
        _recentNoiseSamples.value = emptyList()
        _wakeUpAlert.value = false
    }

    // Trigger simulation spike (for kid-interactive tapping)
    fun tapMonsterSpike() {
        audioMonitor.simulateSpike(85f)
    }

    // Export local SQLite data to JSON format in a file and return local content
    fun exportLocalData(): String {
        val dateFormatFull = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("--- QUIET MONSTER CLASSROOM DATA EXPORT ---\n")
        sb.append("Export Time: ${dateFormatFull.format(Date())}\n")
        sb.append("Local-First Secure Storage (Offline Privacy Mode)\n\n")

        sb.append("=== ALL SESSIONS RECORDED ===\n")
        val sessions = allSessionLogs.value
        if (sessions.isEmpty()) {
            sb.append("No recorded sessions yet.\n")
        } else {
            sb.append("Date | Target Duration | Resets | Status | Max Noise | Avg Noise\n")
            sessions.forEach { s ->
                val dateStr = dateFormatFull.format(Date(s.id))
                val status = if (s.successful) "SUCCESS" else "RESET ${s.resetsCount} TIMES"
                sb.append("$dateStr | ${s.durationSeconds}s | ${s.resetsCount} | $status | ${"%.1f".format(s.maxNoise)}% | ${"%.1f".format(s.averageNoise)}%\n")
            }
        }

        sb.append("\n=== RECENT NOISE DATA SAMPLE POINTS ===\n")
        val noise = allNoiseLogs.value.take(100) // export top 100 recent entries
        if (noise.isEmpty()) {
            sb.append("No noise data points sampled yet.\n")
        } else {
            sb.append("Timestamp | Noise Level | Threshold\n")
            noise.forEach { n ->
                val timeStr = dateFormatFull.format(Date(n.timestamp))
                sb.append("$timeStr | ${"%.1f".format(n.amplitude)}% | ${"%.1f".format(n.threshold)}%\n")
            }
        }

        return sb.toString()
    }

    fun clearAllClassroomData() {
        viewModelScope.launch {
            repository.clearAllData()
            _recentNoiseSamples.value = emptyList()
            _resetsCount.value = 0
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioMonitor.stopMonitoring()
    }
}

class QuietMonsterViewModelFactory(
    private val application: Application,
    private val repository: ClassroomRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuietMonsterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuietMonsterViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
