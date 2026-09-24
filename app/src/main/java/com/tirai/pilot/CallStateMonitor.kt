package com.tirai.pilot

import android.content.Context
import android.content.pm.PackageManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager

class CallStateMonitor(
    private val context: Context,
    private val onCallState: (Boolean) -> Unit
) {
    private val tm = context.getSystemService(TelephonyManager::class.java)

    private val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            onCallState(state != TelephonyManager.CALL_STATE_IDLE)
        }
    }

    fun start() {
        if (context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) return
        tm.registerTelephonyCallback(context.mainExecutor, callback)
    }

    fun stop() {
        runCatching { tm.unregisterTelephonyCallback(callback) }
    }
}
