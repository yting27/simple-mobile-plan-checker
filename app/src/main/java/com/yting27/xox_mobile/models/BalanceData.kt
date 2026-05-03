package com.yting27.xox_mobile.models

import kotlinx.serialization.Serializable

@Serializable
data class BalanceData(
    val totalBalance: String,
    val activeTill: String,
    val dataAmount: String,
    val dataExp: String,
    val seasonPass: String,
    val lastUpdated: Long = System.currentTimeMillis()
)