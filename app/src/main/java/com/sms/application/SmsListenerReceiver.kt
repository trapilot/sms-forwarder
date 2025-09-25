package com.sms.application

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telephony.SmsMessage
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class SmsListenerReceiver : BroadcastReceiver() {

    @RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (smsMessage in messages) {
                if (checkIsOTP(smsMessage)) {
                    val msgContent = "[${smsMessage.displayOriginatingAddress}] ${smsMessage.messageBody}"
                    val sender = smsMessage.displayOriginatingAddress
                    val notificationId = System.currentTimeMillis().toInt()

                    if (isAppInForeground(context)) {
                        // App foreground: open dialog
                        val dialogIntent = Intent(context, MessageDialogActivity::class.java).apply {
                            putExtra("sms_content", msgContent)
                            putExtra("phone_number", sender)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(dialogIntent)
                    } else {
                        // Delay 5s using Handler
                        Handler(Looper.getMainLooper()).postDelayed(@androidx.annotation.RequiresPermission(
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) {
                            // App background: send notification (with copy on click)
                            sendOTPNotification(context, sender, msgContent, notificationId)
                        }, 5000)
                    }
                }
            }
        }
    }

    private fun checkIsOTP(smsMessage: SmsMessage): Boolean {
        val isDebug = false
        val isFromBank = smsMessage.displayOriginatingAddress.contains("Techcombank", ignoreCase = true)
        val containsOTP = smsMessage.messageBody.contains("OTP:", ignoreCase = true)
        return isDebug || (isFromBank && containsOTP)
    }

    @RequiresPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    private fun sendOTPNotification(context: Context, from: String, message: String, notificationId: Int) {
        val channelId = "otp_channel"

        // Create notification channel if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "OTP Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification for OTP messages"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // Intent for notification click: broadcast receiver that copies OTP and cancels auto-send
        val copyIntent = Intent(context, SmsForwarderReceiver::class.java).apply {
            putExtra("sms_content", message)
            putExtra("notification_id", notificationId)
            action = "sms.action.COPY_OTP"
        }
        val pendingCopyIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your icon
            .setContentTitle("OTP từ $from")
            .setContentText("Nhấn để copy OTP vào clipboard")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingCopyIntent)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(notificationId, builder.build())

        // Schedule auto-send after 5s using AutoSendManager
        val taskKey = "auto_send_$notificationId"
        AutoSendManager.schedule(taskKey, 5000) {
            val autoIntent = Intent(context, SmsForwarderReceiver::class.java).apply {
                putExtra("sms_content", message)
                putExtra("phone_number", from)
                action = "sms.action.ACTION_FORWARD"
            }
            context.sendBroadcast(autoIntent)
            NotificationManagerCompat.from(context).cancel(notificationId)
        }
    }

    private fun isAppInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = context.packageName
        return runningProcesses.any { it.processName == packageName && it.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND }
    }
}
