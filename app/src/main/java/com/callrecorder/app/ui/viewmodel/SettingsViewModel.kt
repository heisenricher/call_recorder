package com.callrecorder.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callrecorder.app.data.preferences.AppPreferences
import com.callrecorder.app.data.repository.CallRecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: CallRecordingRepository,
    private val appPreferences: AppPreferences
) : ViewModel() {

    val recordingCount: StateFlow<Int> = repository.recordingCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalStorageUsed: StateFlow<Long> = repository.totalStorageUsed
        .map { it ?: 0L }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val storagePath: StateFlow<String> = appPreferences.storagePath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferences.getDefaultStorageDirectory())

    val isDisclaimerAccepted: StateFlow<Boolean> = appPreferences.isDisclaimerAccepted
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setDisclaimerAccepted(accepted: Boolean) {
        viewModelScope.launch {
            appPreferences.setDisclaimerAccepted(accepted)
        }
    }

    fun setStoragePath(path: String) {
        viewModelScope.launch {
            appPreferences.setStoragePath(path)
        }
    }
}
