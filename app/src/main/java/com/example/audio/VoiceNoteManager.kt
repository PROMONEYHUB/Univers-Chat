package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ln
import kotlin.math.max

data class RecordedVoiceFile(
    val file: File,
    val durationSec: Int
)

@Immutable
data class VoicePlaybackState(
    val activeMessageId: String? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val progress: Float = 0f,
    val positionSec: Int = 0,
    val durationSec: Int = 0,
    val errorMessage: String? = null
)

class VoiceNoteRecorder(context: Context) {
    private val appContext = context.applicationContext
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAtMs: Long = 0L

    fun start(): Result<Unit> = runCatching {
        stopInternal(deleteFile = true)
        val file = File(appContext.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(appContext)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        mr.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(1)
            setAudioSamplingRate(16_000)
            setAudioEncodingBitRate(12_000)
            setMaxDuration(MAX_DURATION_MS)
            setMaxFileSize(MAX_FILE_SIZE_BYTES)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mr
        outputFile = file
        startedAtMs = System.currentTimeMillis()
    }

    /**
     * Reads normalized live microphone amplitude in [0f, 1f] for waveform metering.
     */
    fun currentNormalizedAmplitude(): Float {
        val raw = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        if (raw <= 0) return 0.08f
        val normalized = (ln(raw.toDouble() + 1.0) / ln(32768.0)).toFloat()
        return normalized.coerceIn(0.08f, 1f)
    }

    /**
     * Stops recording and returns the local AAC `.m4a` file ready for Firestore inline Base64 encoding.
     */
    fun stopRecording(): Result<RecordedVoiceFile> = runCatching {
        val file = outputFile ?: error("No active voice recording.")
        val elapsedMs = max(0L, System.currentTimeMillis() - startedAtMs)
        val elapsedSec = ((elapsedMs + 450L) / 1000L).toInt().coerceIn(1, MAX_DURATION_SEC)
        try {
            recorder?.stop()
        } catch (err: Exception) {
            file.delete()
            outputFile = null
            throw IllegalStateException("Recording was too short. Speak a little longer.", err)
        } finally {
            runCatching { recorder?.release() }
            recorder = null
        }
        outputFile = null
        require(file.exists() && file.length() >= 256L) {
            file.delete()
            "Recording was too short. Hold or speak a little longer."
        }
        RecordedVoiceFile(file = file, durationSec = elapsedSec)
    }

    fun cancel() {
        stopInternal(deleteFile = true)
    }

    private fun stopInternal(deleteFile: Boolean) {
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null
        if (deleteFile) {
            outputFile?.delete()
        }
        outputFile = null
    }

    companion object {
        const val MAX_DURATION_SEC = 40
        const val MAX_DURATION_MS = MAX_DURATION_SEC * 1000
        private const val MAX_FILE_SIZE_BYTES = 68_000L
        const val MAX_AUDIO_BASE64_LENGTH = 95_000

        /**
         * Encodes a recorded `.m4a` voice file into a Firestore-safe Base64 string.
         */
        fun encodeFileToBase64(file: File): Result<String> = runCatching {
            require(file.exists() && file.length() >= 128L) {
                "Recorded audio file is missing or empty."
            }
            val bytes = file.readBytes()
            require(bytes.size <= 71_000) {
                "Voice message is too long. Please keep recordings under $MAX_DURATION_SEC seconds."
            }
            val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
            require(encoded.length in 16..MAX_AUDIO_BASE64_LENGTH) {
                "Voice message exceeds Firestore size limit."
            }
            encoded
        }
    }
}

class VoiceNotePlayer(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaPlayer: MediaPlayer? = null
    private var tempFile: File? = null
    private var progressJob: Job? = null

    private val _state = MutableStateFlow(VoicePlaybackState())
    val state: StateFlow<VoicePlaybackState> = _state.asStateFlow()

    /**
     * Toggles playback for a voice message backed by either a Firebase Storage HTTPS URL
     * (`audioUrl`) or a legacy inline Base64 payload (`audioBase64`).
     */
    fun togglePlayback(
        messageId: String,
        audioUrl: String? = null,
        audioBase64: String? = null
    ) {
        val current = _state.value
        val player = mediaPlayer
        if (current.activeMessageId == messageId && player != null) {
            if (current.isBuffering) {
                stop()
                return
            }
            if (player.isPlaying) {
                player.pause()
                progressJob?.cancel()
                _state.value = current.copy(isPlaying = false)
            } else {
                player.start()
                _state.value = current.copy(isPlaying = true)
                startProgressLoop(messageId, player)
            }
            return
        }

        stop()

        val cleanUrl = audioUrl?.trim()?.takeIf {
            it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true)
        }
        if (cleanUrl != null) {
            playRemoteUrl(messageId, cleanUrl)
        } else if (!audioBase64.isNullOrBlank()) {
            playLegacyBase64(messageId, audioBase64)
        }
    }

    private fun playRemoteUrl(messageId: String, url: String) {
        runCatching {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { prepared ->
                    if (mediaPlayer !== prepared) {
                        runCatching { prepared.release() }
                        return@setOnPreparedListener
                    }
                    val totalSec = (prepared.duration / 1000).coerceAtLeast(1)
                    prepared.start()
                    _state.value = VoicePlaybackState(
                        activeMessageId = messageId,
                        isPlaying = true,
                        isBuffering = false,
                        progress = 0f,
                        positionSec = 0,
                        durationSec = totalSec
                    )
                    startProgressLoop(messageId, prepared)
                }
                setOnCompletionListener {
                    stop()
                }
                setOnErrorListener { _, _, _ ->
                    stop(errorMessage = "Unable to play audio message")
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
            _state.value = VoicePlaybackState(
                activeMessageId = messageId,
                isPlaying = false,
                isBuffering = true,
                progress = 0f,
                positionSec = 0
            )
        }.onFailure {
            stop(errorMessage = "Unable to play audio message")
        }
    }

    private fun playLegacyBase64(messageId: String, audioBase64: String) {
        runCatching {
            val bytes = Base64.decode(audioBase64, Base64.DEFAULT)
            val file = File(appContext.cacheDir, "play_${messageId.take(12)}.m4a")
            FileOutputStream(file).use { it.write(bytes) }
            tempFile = file

            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    stop()
                }
                setOnErrorListener { _, _, _ ->
                    stop(errorMessage = "Unable to play audio message")
                    true
                }
                start()
            }
            mediaPlayer = mp
            val totalSec = (mp.duration / 1000).coerceAtLeast(1)
            _state.value = VoicePlaybackState(
                activeMessageId = messageId,
                isPlaying = true,
                isBuffering = false,
                progress = 0f,
                positionSec = 0,
                durationSec = totalSec
            )
            startProgressLoop(messageId, mp)
        }.onFailure {
            stop(errorMessage = "Unable to play audio message")
        }
    }

    private fun startProgressLoop(messageId: String, player: MediaPlayer) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && mediaPlayer === player) {
                val duration = runCatching { player.duration }.getOrDefault(1).coerceAtLeast(1)
                val currentPos = runCatching { player.currentPosition }.getOrDefault(0).coerceIn(0, duration)
                val playing = runCatching { player.isPlaying }.getOrDefault(false)
                _state.value = VoicePlaybackState(
                    activeMessageId = messageId,
                    isPlaying = playing,
                    isBuffering = false,
                    progress = (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f),
                    positionSec = currentPos / 1000,
                    durationSec = (duration / 1000).coerceAtLeast(1)
                )
                if (!playing) break
                delay(65L)
            }
        }
    }

    fun stop(errorMessage: String? = null) {
        progressJob?.cancel()
        progressJob = null
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        tempFile?.delete()
        tempFile = null
        _state.value = VoicePlaybackState(errorMessage = errorMessage)
    }

    fun release() {
        stop()
    }
}

fun formatDurationSec(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return "%d:%02d".format(mins, secs)
}
