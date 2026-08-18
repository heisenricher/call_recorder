package com.callrecorder.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_recordings")
data class CallRecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String?,
    val contactName: String?,
    val callType: String, // "INCOMING", "OUTGOING", "UNKNOWN"
    val startTime: Long,   // Epoch timestamp ms
    val durationSeconds: Long,
    val filePath: String,
    val fileSize: Long     // Bytes
)
