package com.yting27.xox_mobile.controllers

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.yting27.xox_mobile.models.BalanceData
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.Debug

class SmsService(
    private val activity: ComponentActivity,
    private val onBalanceReceived: (BalanceData?) -> Unit
) {
    companion object {
        private const val XOX_NUM = "22111"
        private const val SENT_ACTION = "com.example.SMS_SENT"
        private const val DELIVERED_ACTION = "com.example.SMS_DELIVERED"
        private const val PREFS_NAME = "XoxPrefs"
        private const val KEY_BALANCE_JSON = "balance_json"
    }

    enum class SmsCommand(val code: String) {
        BALANCE("BAL"),
        TOPUP_INTERNET("DATA ADD 500")
    }

    private val requestSmsPermissions: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                Toast.makeText(activity, "Permissions granted! Please click the button again.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "Permissions denied. SMS features will be unavailable.", Toast.LENGTH_LONG).show()
            }
        }

    fun queryBalance() {
        checkAndSendSms(XOX_NUM, SmsCommand.BALANCE.code)
    }

    fun addInternet() {
        checkAndSendSms(XOX_NUM, SmsCommand.TOPUP_INTERNET.code)
    }

    private fun checkAndSendSms(destination: String, text: String) {
        val permissions = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS)
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            sendSms(destination, text)
        } else {
            requestSmsPermissions.launch(missingPermissions.toTypedArray())
        }
    }

    private fun sendSms(destination: String, text: String) {
        val smsManager = activity.getSystemService(SmsManager::class.java)!!

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }

        val sentIntent = PendingIntent.getBroadcast(
            activity, 0, Intent(SENT_ACTION).setPackage(activity.packageName), flags
        )
        val deliveredIntent = PendingIntent.getBroadcast(
            activity, 0, Intent(DELIVERED_ACTION).setPackage(activity.packageName), flags
        )

        registerStatusReceivers()

        smsManager.sendTextMessage(destination, null, text, sentIntent, deliveredIntent)
    }

    private fun registerStatusReceivers() {
        val sentReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val msg = when (resultCode) {
                    Activity.RESULT_OK -> "Sent"
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Generic failure"
                    SmsManager.RESULT_ERROR_NO_SERVICE -> "No service"
                    SmsManager.RESULT_ERROR_NULL_PDU -> "Null PDU"
                    SmsManager.RESULT_ERROR_RADIO_OFF -> "Radio off"
                    else -> "Failed: $resultCode"
                }
                Toast.makeText(activity, "SMS Status: $msg", Toast.LENGTH_SHORT).show()
            }
        }
        val deliveredReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                // Handle delivery report
            }
        }

        val recvFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_NOT_EXPORTED
        } else 0

        activity.registerReceiver(sentReceiver, IntentFilter(SENT_ACTION), recvFlags)
        activity.registerReceiver(deliveredReceiver, IntentFilter(DELIVERED_ACTION), recvFlags)
    }

    fun registerSmsReceiver() {
        val smsReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    for (message in messages) {
                        val sender = message.displayOriginatingAddress
                        val body = message.displayMessageBody
                        // only parse sms from XOX Number
                        if (sender?.contains(XOX_NUM) == true) {
                            val balance = parseSmsBody(body)
                            if (balance != null) {
                                saveBalance(balance)
                            }
                            onBalanceReceived(balance)
                        }
                    }
                }
            }
        }

        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_EXPORTED
        } else 0
        
        activity.registerReceiver(smsReceiver, filter, flags)
    }

    private fun parseSmsBody(body: String): BalanceData? {
        return try {
            val da = "DA: RM(\\d+\\.\\d+)".toRegex().find(body)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            val ma = "MA: RM(\\d+\\.\\d+)".toRegex().find(body)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            val totalBalance = da + ma
            val activeTill = "Active till ([\\d\\/]+)".toRegex().find(body)?.groupValues?.get(1)?.trim() ?: "N/A"

            val dataMatch = "Data: ([^(]+)\\(Exp: ([^)]+)\\)".toRegex().find(body)
            val dataAmount = dataMatch?.groupValues?.get(1)?.trim() ?: "N/A"
            val dataExp = dataMatch?.groupValues?.get(2)?.trim() ?: "N/A"

            val seasonPass = "SeasonPass: (.+)".toRegex().find(body)?.groupValues?.get(1)?.trim() ?: "N/A"

            BalanceData(
                totalBalance = "RM${"%.2f".format(totalBalance)}",
                activeTill = activeTill,
                dataAmount = dataAmount,
                dataExp = dataExp,
                seasonPass = seasonPass
            )
        } catch (_: Exception) {
            null
        }
    }

    fun saveBalance(balance: BalanceData) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = Json.encodeToString(balance)
        prefs.edit().putString(KEY_BALANCE_JSON, jsonString).apply()
    }

    fun loadBalance(): BalanceData? {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_BALANCE_JSON, null) ?: return null
        return try {
            Json.decodeFromString<BalanceData>(jsonString)
        } catch (_: Exception) {
            null
        }
    }
}
