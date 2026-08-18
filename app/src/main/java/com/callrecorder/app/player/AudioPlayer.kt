package com.callrecorder.app.player

import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val currentPosition: Int = 0,
    val duration: Int = 0,
    val currentFilePath: String? = null
)

class AudioPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    fun play(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            stop()
            return
        }

        // If same file and paused, resume
        if (_playbackState.value.currentFilePath == filePath && _playbackState.value.isPaused) {
            resume()
            return
        }

        stop()

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(filePath)
                prepare()
                start()

                val trackDuration = duration
                _playbackState.value = PlaybackState(
                    isPlaying = true,
                    isPaused = false,
                    currentPosition = 0,
                    duration = trackDuration,
                    currentFilePath = filePath
                )

                setOnCompletionListener {
                    stop()
                }

                setOnErrorListener { _, _, _ ->
                    stop()
                    true
                }
            }

            startProgressTracker()
        } catch (e: Exception) {
            e.printStackTrace()
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    isPaused = true,
                    currentPosition = it.currentPosition
                )
                progressJob?.cancel()
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            if (!it.isPlaying) {
                it.start()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = true,
                    isPaused = false
                )
                startProgressTracker()
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                reset()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaPlayer = null
            _playbackState.value = PlaybackState()
        }
    }

    fun seekTo(positionMs: Int) {
        mediaPlayer?.let {
            it.seekTo(positionMs)
            _playbackState.value = _playbackState.value.copy(currentPosition = positionMs)
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && mediaPlayer != null && mediaPlayer?.isPlaying == true) {
                try {
                    val currentPos = mediaPlayer?.currentPosition ?: 0
                    val dur = mediaPlayer?.duration ?: _playbackState.value.duration
                    _playbackState.value = _playbackState.value.copy(
                        currentPosition = currentPos,
                        duration = dur
                    )
                } catch (e: Exception) {
                    break
                }
                delay(200)
            }
        }
    }
}
