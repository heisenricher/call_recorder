package com.callrecorder.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callrecorder.app.data.db.CallRecordingEntity
import com.callrecorder.app.data.preferences.AppPreferences
import com.callrecorder.app.data.repository.CallRecordingRepository
import com.callrecorder.app.player.AudioPlayer
import com.callrecorder.app.player.PlaybackState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CallRecordingRepository,
    private val appPreferences: AppPreferences,
    private val audioPlayer: AudioPlayer
) : ViewModel() {

    val recordings: StateFlow<List<CallRecordingEntity>> = repository.allRecordings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isRecordingEnabled: StateFlow<Boolean> = appPreferences.isRecordingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val playbackState: StateFlow<PlaybackState> = audioPlayer.playbackState

    fun toggleRecordingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setRecordingEnabled(enabled)
        }
    }

    fun playRecording(filePath: String) {
        audioPlayer.play(filePath)
    }

    fun pausePlayback() {
        audioPlayer.pause()
    }

    fun resumePlayback() {
        audioPlayer.resume()
    }

    fun seekPlayback(positionMs: Int) {
        audioPlayer.seekTo(positionMs)
    }

    fun stopPlayback() {
        audioPlayer.stop()
    }

    fun deleteRecording(recording: CallRecordingEntity) {
        viewModelScope.launch {
            if (audioPlayer.playbackState.value.currentFilePath == recording.filePath) {
                audioPlayer.stop()
            }
            repository.deleteRecording(recording)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }
}
