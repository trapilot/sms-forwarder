package com.sms.application

import android.os.Handler
import android.os.Looper

object AutoSendManager {
    private val handler = Handler(Looper.getMainLooper())
    private val taskMap = mutableMapOf<String, Runnable>()

    fun schedule(key: String, delayMillis: Long, action: () -> Unit) {
        val runnable = Runnable {
            taskMap.remove(key)
            action()
        }
        taskMap[key] = runnable
        handler.postDelayed(runnable, delayMillis)
    }

    fun cancel(key: String) {
        taskMap[key]?.let {
            handler.removeCallbacks(it)
            taskMap.remove(key)
        }
    }

    fun extractOTP(smsContent: String): String {
        val numberRegex = "\\d+".toRegex()
        val allNumberBlocks = numberRegex.findAll(smsContent).map { it.value }.toList()

        val otpCandidate = allNumberBlocks.find { it.length in 4..6 }

        return otpCandidate ?: "NA"
    }
}
