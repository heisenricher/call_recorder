package com.callrecorder.app.data.repository

import com.callrecorder.app.data.db.CallRecordingDao
import com.callrecorder.app.data.db.CallRecordingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRecordingRepository @Inject constructor(
    private val callRecordingDao: CallRecordingDao
) {

    val allRecordings: Flow<List<CallRecordingEntity>> = callRecordingDao.getAllRecordings()

    val recordingCount: Flow<Int> = callRecordingDao.getRecordingCount()

    val totalStorageUsed: Flow<Long?> = callRecordingDao.getTotalStorageUsed()

    suspend fun getRecordingById(id: Long): CallRecordingEntity? {
        return withContext(Dispatchers.IO) {
            callRecordingDao.getRecordingById(id)
        }
    }

    suspend fun insertRecording(recording: CallRecordingEntity): Long {
        return withContext(Dispatchers.IO) {
            callRecordingDao.insertRecording(recording)
        }
    }

    suspend fun deleteRecording(recording: CallRecordingEntity) {
        withContext(Dispatchers.IO) {
            // Delete physical file from storage
            try {
                val file = File(recording.filePath)
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Delete database row
            callRecordingDao.deleteRecording(recording)
        }
    }

    suspend fun deleteRecordingById(id: Long) {
        withContext(Dispatchers.IO) {
            val recording = callRecordingDao.getRecordingById(id)
            if (recording != null) {
                deleteRecording(recording)
            }
        }
    }
}
