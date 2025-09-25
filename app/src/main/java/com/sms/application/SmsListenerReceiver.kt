package com.sms.application

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log

class SmsListenerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (smsMessage in messages) {
                if (this.checkIsOTP(smsMessage)) {
                    val msgContent = "[${smsMessage.displayOriginatingAddress}] ${smsMessage.messageBody}"

                    // Launch the MessageDialogActivity to show the dialog
                    val dialogIntent = Intent(context, MessageDialogActivity::class.java).apply {
                        putExtra("sms_content", msgContent)
                        putExtra("phone_number", smsMessage.displayOriginatingAddress)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(dialogIntent)
                }
            }
        }
    }

    private fun checkIsOTP(smsMessage: SmsMessage): Boolean {
        val isDebug = false
        val isFromBank = smsMessage.displayOriginatingAddress.contains("Techcombank", ignoreCase = true)
        val containsOTP = smsMessage.messageBody.contains("OTP:", ignoreCase = true)

        Log.e("[DEBUG]", "SMS check: ${isFromBank} ${containsOTP}")
        return isDebug || (isFromBank && containsOTP)
    }
}