package com.yting27.xox_mobile.repositories

import android.content.Context
import com.yting27.xox_mobile.models.BalanceData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BalanceRepository(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "XoxPrefs"
        private const val KEY_BALANCE_JSON = "balance_json"
    }

    fun saveBalance(balance: BalanceData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = Json.encodeToString(balance)
        prefs.edit().putString(KEY_BALANCE_JSON, jsonString).apply()
    }

    fun loadBalance(): BalanceData? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_BALANCE_JSON, null) ?: return null
        return try {
            Json.decodeFromString<BalanceData>(jsonString)
        } catch (_: Exception) {
            null
        }
    }
}
