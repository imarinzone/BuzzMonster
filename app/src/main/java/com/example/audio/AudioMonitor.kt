package com.example.audio

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class AudioMonitor(private val context: Context) {

    private val _noiseLevel = MutableStateFlow(0f)
    val noiseLevel: StateFlow<Float> = _noiseLevel.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isSimulatorMode = MutableStateFlow(false)
    val isSimulatorMode: StateFlow<Boolean> = _isSimulatorMode.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun startMonitoring() {
        if (recordingJob != null) return

        if (!hasAudioPermission()) {
            // No permission, run in simulator mode
            startSimulator()
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize.coerceAtLeast(3200)
            )

            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()
                _isRecording.value = true
                _isSimulatorMode.value = false

                recordingJob = scope.launch {
                    val buffer = ShortArray(1024)
                    var smoothedLevel = 0f

                    while (isActive) {
                        val readResult = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (readResult > 0) {
                            var maxAbs = 0
                            for (i in 0 until readResult) {
                                val value = abs(buffer[i].toInt())
                                if (value > maxAbs) {
                                    maxAbs = value
                                }
                            }

                            // Scale to 0 - 100
                            val measuredLevel = (maxAbs / 32768.0f) * 100f

                            // Apply moving average smoothing
                            smoothedLevel = (smoothedLevel * 0.6f) + (measuredLevel * 0.4f)
                            _noiseLevel.value = smoothedLevel.coerceIn(0f, 100f)
                        }
                        delay(50) // Read approx 20 times per second
                    }
                }
            } else {
                // If initialization failed (e.g. running on an emulator with no audio card)
                startSimulator()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            startSimulator()
        }
    }

    private fun startSimulator() {
        _isSimulatorMode.value = true
        _isRecording.value = true
        recordingJob = scope.launch {
            var baseNoise = 15f
            while (isActive) {
                // Generate natural background classroom ambient noise
                val fluctuation = (Math.random() - 0.5) * 8f
                baseNoise = (baseNoise + fluctuation.toFloat()).coerceIn(10f, 30f)

                // Occasional random kid giggle or pencil drop
                val spike = if (Math.random() < 0.05) (Math.random() * 40f).toFloat() else 0f

                _noiseLevel.value = (baseNoise + spike).coerceIn(0f, 100f)
                delay(100)
            }
        }
    }

    // Trigger a temporary manual noise spike in simulator mode (e.g., when clicking the monster to play)
    fun simulateSpike(amount: Float) {
        if (_isSimulatorMode.value) {
            _noiseLevel.value = amount.coerceIn(0f, 100f)
        }
    }

    fun stopMonitoring() {
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioRecord = null
            _isRecording.value = false
            _noiseLevel.value = 0f
        }
    }
}
