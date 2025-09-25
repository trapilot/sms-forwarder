package com.sms.application

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.content.Intent
import android.os.Handler
import android.os.Looper

class MessageDialogActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var autoSendRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_dialog)

        val smsContent = intent.getStringExtra("sms_content") ?: ""
        val phoneNumber = intent.getStringExtra("phone_number") ?: "Unknown"

        val messageTextView = findViewById<TextView>(R.id.messageTextView)
        messageTextView.text = "Bạn có muốn gửi tới số điện thoại: $phoneNumber?"

        val yesButton = findViewById<Button>(R.id.btnYes)
        val noButton = findViewById<Button>(R.id.btnNo)

        // Runnable automatically selects "Yes" after xx seconds
        autoSendRunnable = Runnable {
            forwardSMS(phoneNumber, smsContent)
            finish()
        }
        handler.postDelayed(autoSendRunnable!!, 5_000) // 10s

        yesButton.setOnClickListener {
            cancelAutoSend()
            forwardSMS(phoneNumber, smsContent)
            finish()
        }

        noButton.setOnClickListener {
            cancelAutoSend()
            copyToClipboard(smsContent)
            finish()
        }
    }

    private fun cancelAutoSend() {
        autoSendRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAutoSend()
    }

    private fun forwardSMS(phoneNumber: String, smsContent: String) {
        val forwardIntent = Intent(this, SmsForwarderReceiver::class.java).apply {
            putExtra("sms_content", smsContent)
            putExtra("phone_number", phoneNumber)
            action = "sms.action.ACTION_FORWARD"
        }
        sendBroadcast(forwardIntent)
    }

    private fun copyToClipboard(smsContent: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val otpCode = extractOTP(smsContent)

        val clip = ClipData.newPlainText("OTP", otpCode)
        clipboard.setPrimaryClip(clip)
    }

    private fun extractOTP(smsContent: String): String {
        val numberRegex = "\\d+".toRegex()
        val allNumberBlocks = numberRegex.findAll(smsContent).map { it.value }.toList()

        val otpCandidate = allNumberBlocks.find { it.length in 4..6 }

        return otpCandidate ?: "NA"
    }
}
