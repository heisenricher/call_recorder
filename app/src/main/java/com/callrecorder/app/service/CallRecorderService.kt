package com.callrecorder.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.database.Cursor
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.app.NotificationCompat
import com.callrecorder.app.CallRecorderApp
import com.callrecorder.app.MainActivity
import com.callrecorder.app.R
import com.callrecorder.app.data.db.CallRecordingEntity
import com.callrecorder.app.data.preferences.AppPreferences
import com.callrecorder.app.data.repository.CallRecordingRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class CallRecorderService : Service() {

    @Inject
    lateinit var repository: CallRecordingRepository

    @Inject
    lateinit var appPreferences: AppPreferences

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var recordingStartTime = 0L
    private var currentFilePath: String? = null
    private var currentPhoneNumber: String? = null
    private var currentCallType: String = "UNKNOWN"

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val ACTION_START_RECORDING = "com.callrecorder.app.ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.callrecorder.app.ACTION_STOP_RECORDING"
        
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_CALL_TYPE = "extra_call_type" // "INCOMING", "OUTGOING", "UNKNOWN"
        
        private const val NOTIFICATION_ID = 1001

        fun startRecording(context: Context, phoneNumber: String?, callType: String) {
            val intent = Intent(context, CallRecorderService::class.java).apply {
                action = ACTION_START_RECORDING
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
                putExtra(EXTRA_CALL_TYPE, callType)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, CallRecorderService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER)
                val callType = intent.getStringExtra(EXTRA_CALL_TYPE) ?: "UNKNOWN"
                handleStartRecording(phoneNumber, callType)
            }
            ACTION_STOP_RECORDING -> {
                handleStopRecording()
            }
        }
        return START_NOT_STICKY
    }

    private fun handleStartRecording(phoneNumber: String?, callType: String) {
        if (isRecording) return

        serviceScope.launch {
            val isEnabled = appPreferences.isRecordingEnabled.first()
            if (!isEnabled) {
                stopSelf()
                return@launch
            }

            currentPhoneNumber = phoneNumber
            currentCallType = callType
            recordingStartTime = System.currentTimeMillis()

            startForegroundWithNotification()

            val storageDir = appPreferences.storagePath.first()
            val dir = File(storageDir)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val typePrefix = if (callType == "INCOMING") "IN" else if (callType == "OUTGOING") "OUT" else "CALL"
            val sanitizedNumber = phoneNumber?.replace(Regex("[^0-9+]"), "") ?: "Unknown"
            val fileName = "${typePrefix}_${sanitizedNumber}_${timestamp}.mp3"
            val outputFile = File(dir, fileName)
            currentFilePath = outputFile.absolutePath

            startMediaRecorder(outputFile.absolutePath)
        }
    }

    private fun startMediaRecorder(filePath: String) {
        try {
            // Try VOICE_COMMUNICATION first, fallback to MIC
            var initialized = initRecorder(MediaRecorder.AudioSource.VOICE_COMMUNICATION, filePath)
            if (!initialized) {
                initialized = initRecorder(MediaRecorder.AudioSource.MIC, filePath)
            }

            if (initialized) {
                mediaRecorder?.start()
                isRecording = true
            } else {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            cleanupRecorder()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun initRecorder(audioSource: Int, filePath: String): Boolean {
        return try {
            cleanupRecorder()
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(audioSource)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(filePath)
                prepare()
            }
            mediaRecorder = recorder
            true
        } catch (e: Exception) {
            e.printStackTrace()
            cleanupRecorder()
            false
        }
    }

    private fun handleStopRecording() {
        if (!isRecording) {
            stopSelf()
            return
        }

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
            isRecording = false
        }

        val endTime = System.currentTimeMillis()
        val durationSeconds = ((endTime - recordingStartTime) / 1000).coerceAtLeast(0)
        val path = currentFilePath

        if (path != null) {
            serviceScope.launch {
                val file = File(path)
                if (file.exists() && file.length() > 0) {
                    val fileSize = file.length()
                    
                    // Resolve phone number if not available
                    val finalPhoneNumber = currentPhoneNumber ?: resolveLastCallNumber()
                    val contactName = finalPhoneNumber?.let { resolveContactName(it) }

                    val recording = CallRecordingEntity(
                        phoneNumber = finalPhoneNumber,
                        contactName = contactName,
                        callType = currentCallType,
                        startTime = recordingStartTime,
                        durationSeconds = durationSeconds,
                        filePath = path,
                        fileSize = fileSize
                    )

                    repository.insertRecording(recording)
                } else if (file.exists() && file.length() == 0L) {
                    file.delete()
                }

                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        } else {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startForegroundWithNotification() {
        val notification = buildSilentNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildSilentNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CallRecorderApp.RECORDING_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.recording_notification_title))
            .setContentText(getString(R.string.recording_notification_text))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun cleanupRecorder() {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
        }
    }

    private fun resolveLastCallNumber(): String? {
        return try {
            val cursor: Cursor? = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.TYPE),
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT 1"
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                    if (numberIndex != -1) it.getString(numberIndex) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveContactName(phoneNumber: String): String? {
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val cursor: Cursor? = contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIndex != -1) it.getString(nameIndex) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroy() {
        handleStopRecording()
        super.onDestroy()
    }
}
