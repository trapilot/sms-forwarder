package com.sms.application

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi

class SmsForwarderReceiver : BroadcastReceiver() {
    @SuppressLint("ObsoleteSdkInt")
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        when (action) {
            "sms.action.ACTION_SENT" -> {
                when (resultCode) {
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> {
                        Log.e("[DEBUG]", "SMS gửi thất bại: Lỗi chung")
                        Toast.makeText(context, "Gửi tin nhắn thất bại: Lỗi chung!", Toast.LENGTH_SHORT).show()
                    }
                    SmsManager.RESULT_ERROR_NO_SERVICE -> {
                        Log.e("[DEBUG]", "SMS gửi thất bại: Không có dịch vụ")
                        Toast.makeText(context, "Gửi tin nhắn thất bại: Không có dịch vụ!", Toast.LENGTH_SHORT).show()
                    }
                    SmsManager.RESULT_ERROR_RADIO_OFF -> {
                        Log.e("[DEBUG]", "SMS gửi thất bại: Radio tắt")
                        Toast.makeText(context, "Gửi tin nhắn thất bại: Radio tắt!", Toast.LENGTH_SHORT).show()
                    }
                    SmsManager.RESULT_ERROR_NULL_PDU -> {
                        Log.e("[DEBUG]", "SMS gửi thất bại: PDU rỗng")
                        Toast.makeText(context, "Gửi tin nhắn thất bại: PDU rỗng!", Toast.LENGTH_SHORT).show()
                    }
                    SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED -> {
                        Log.e("[DEBUG]", "SMS gửi thất bại: Không được phép gửi mã ngắn")
                        Toast.makeText(context, "Gửi tin nhắn thất bại: Không được phép gửi mã ngắn!", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Log.i("[DEBUG]", "SMS đã gửi thành công")
                        Toast.makeText(context, "Đã chuyển tiếp tin nhắn thành công!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            "sms.action.ACTION_FORWARD" -> {
                val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val forwardNumber = sharedPref.getString("forward_number", "")
                val smsContent = intent.getStringExtra("sms_content")

                if (forwardNumber.isNullOrEmpty() || smsContent.isNullOrEmpty()) {
                    Toast.makeText(context, "Không thể chuyển tiếp: Thiếu thông tin!", Toast.LENGTH_SHORT).show()
                    return
                }

                Log.i("[DEBUG]", "$forwardNumber $smsContent")

                try {
                    val sentIntent = PendingIntent.getBroadcast(
                        context,
                        0,
                        Intent("sms.action.ACTION_SENT").apply {
                            putExtra(
                                "sms_content",
                                smsContent
                            )
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val subscriptionId = SubscriptionManager.getDefaultSmsSubscriptionId()
                        val smsManager = context.getSystemService(SmsManager::class.java)
                            .createForSubscriptionId(subscriptionId)

                        smsManager.sendTextMessage(
                            forwardNumber,
                            null,
                            smsContent,
                            sentIntent,
                            null
                        )
                    } else {
                        val smsManager = SmsManager.getDefault()
                        smsManager.sendTextMessage(
                            forwardNumber,
                            null,
                            smsContent,
                            sentIntent,
                            null
                        )
                    }
                    Toast.makeText(context, "Đã chuyển tới $forwardNumber!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e("[DEBUG]", "Gửi SMS thất bại: ${e.message}")
                    Toast.makeText(context, "Gửi tin nhắn thất bại!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}