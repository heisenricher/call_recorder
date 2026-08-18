package com.callrecorder.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import java.util.concurrent.Executors

class CallDetectorAccessibilityService : AccessibilityService() {

    private var telephonyManager: TelephonyManager? = null
    private var isCallActive = false
    private var isIncoming = false
    private var incomingNumber: String? = null

    // For Android 12+ (API 31+)
    private var telephonyCallback: TelephonyCallback? = null
    
    // For pre-API 31
    private var phoneStateListener: PhoneStateListener? = null

    companion object {
        var isServiceRunning = false
            private set
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
        registerTelephonyListener()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return

        // Check for common dialer/in-call UI packages
        if (isInCallPackage(packageName)) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                // InCall UI appeared or changed
                // Telephony callback handles the actual start/stop based on precise audio hook state
            }
        }
    }

    private fun isInCallPackage(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower.contains("incallui") ||
               lower.contains("dialer") ||
               lower.contains("telecom") ||
               lower.contains("phone")
    }

    override fun onInterrupt() {
        // Nothing required on interrupt
    }

    private fun registerTelephonyListener() {
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            registerTelephonyCallbackApi31()
        } else {
            registerPhoneStateListenerLegacy()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun registerTelephonyCallbackApi31() {
        val executor = Executors.newSingleThreadExecutor()
        val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                handleCallStateChange(state, null)
            }
        }
        telephonyCallback = callback
        try {
            telephonyManager?.registerTelephonyCallback(executor, callback)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    @Suppress("DEPRECATION")
    private fun registerPhoneStateListenerLegacy() {
        phoneStateListener = object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                handleCallStateChange(state, phoneNumber)
            }
        }
        try {
            telephonyManager?.listen(
                phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun handleCallStateChange(state: Int, number: String?) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                // Incoming call ringing
                isIncoming = true
                incomingNumber = number
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> {
                // Call answered or outgoing call started
                if (!isCallActive) {
                    isCallActive = true
                    val callType = if (isIncoming) "INCOMING" else "OUTGOING"
                    val phone = incomingNumber ?: number
                    CallRecorderService.startRecording(this, phone, callType)
                }
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                // Call ended or hung up
                if (isCallActive) {
                    isCallActive = false
                    CallRecorderService.stopRecording(this)
                }
                // Reset tracking state
                isIncoming = false
                incomingNumber = null
            }
        }
    }

    override fun onDestroy() {
        isServiceRunning = false
        unregisterTelephonyListener()
        super.onDestroy()
    }

    private fun unregisterTelephonyListener() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback?.let {
                    telephonyManager?.unregisterTelephonyCallback(it)
                }
            } else {
                phoneStateListener?.let {
                    @Suppress("DEPRECATION")
                    telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
