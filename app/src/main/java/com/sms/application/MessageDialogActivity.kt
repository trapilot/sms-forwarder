package com.sms.application

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView

class MessageDialogActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private var autoSendRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_dialog)

        val smsContent = intent.getStringExtra("sms_content") ?: ""
        val phoneNumber = intent.getStringExtra("phone_number") ?: "Unknown"
        val notificationId = intent.getStringExtra("notification_id") ?: ""

        // Cancel auto-send if opened from notification
        if (notificationId.isNotEmpty()) {
            AutoSendManager.cancel("auto_send_$notificationId")
        }

        val messageTextView = findViewById<TextView>(R.id.messageTextView)
        messageTextView.text = "Bạn có muốn gửi tới số điện thoại: $phoneNumber?"

        val yesButton = findViewById<Button>(R.id.btnYes)
        val noButton = findViewById<Button>(R.id.btnNo)

        // Runnable automatically selects "Yes" after 5 seconds
        autoSendRunnable = Runnable {
            forwardSMS(phoneNumber, smsContent)
            finish()
        }
        handler.postDelayed(autoSendRunnable!!, 5000)

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
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val otpCode = AutoSendManager.extractOTP(smsContent)

        val clip = ClipData.newPlainText("OTP", otpCode)
        clipboard.setPrimaryClip(clip)
    }
}
