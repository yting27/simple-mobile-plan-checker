package com.yting27.xox_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SystemSecurityUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.yting27.xox_mobile.controllers.SmsService
import com.yting27.xox_mobile.models.BalanceData
import com.yting27.xox_mobile.repositories.BalanceRepository
import com.yting27.xox_mobile.ui.components.InfoCard
import com.yting27.xox_mobile.ui.theme.Xox_mobileTheme


class HomePage : ComponentActivity() {
    private var balanceData by mutableStateOf<BalanceData?>(null)
    private lateinit var smsService: SmsService
    private lateinit var balanceRepository: BalanceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        balanceRepository = BalanceRepository(this)
        smsService = SmsService(this, balanceRepository) { balance ->
            balanceData = balance
        }
        smsService.registerSmsReceiver()
        balanceData = balanceRepository.loadBalance()
        setContent {
            Xox_mobileTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CustomTopAppBar()
                    },
                ) { innerPadding ->
                    FunctionalityRegion(
                        modifier = Modifier.padding(innerPadding),
                        balanceData = balanceData,
                        onSendClick = { smsService.queryBalance() },
                        onAddInternetClick = { smsService.addInternet() }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomTopAppBar() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .height(64.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mobile Plan Query",
                style = MaterialTheme.typography.titleLarge
            )
        }
    }
}

@Composable
fun FunctionalityRegion(
    modifier: Modifier = Modifier,
    balanceData: BalanceData? = null,
    onSendClick: () -> Unit = {},
    onAddInternetClick: () -> Unit = {}
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Add Data") },
            text = { Text("Are you sure you want to add 500MB data?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onAddInternetClick()
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Queried balance info
        if (balanceData != null) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val lastUpdatedStr = dateFormat.format(Date(balanceData.lastUpdated))
            Text(
                text = "Last updated: $lastUpdatedStr",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            InfoCard(
                title = "Credit Balance",
                value = balanceData.totalBalance,
                subValue = "Active Till: ${balanceData.activeTill}",
                icon = Icons.Default.Info,
                color = MaterialTheme.colorScheme.primaryContainer
            )
            InfoCard(
                title = "Remaining Data",
                value = balanceData.dataAmount,
                subValue = "Expires: ${balanceData.dataExp}",
                icon = Icons.Default.Refresh,
                color = MaterialTheme.colorScheme.secondaryContainer
            )
            InfoCard(
                title = "Season Pass",
                value = balanceData.seasonPass,
                icon = Icons.Default.ShoppingCart,
                color = MaterialTheme.colorScheme.tertiaryContainer
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = "No data yet. Please request balance.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Check balance
            Button(
                onClick = onSendClick,
                modifier = Modifier.weight(1f),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Icon(Icons.Default.SystemSecurityUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Check Balance")
            }
            // Top-up internet
            Button(
                onClick = { showConfirmDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Buy Data")
            }
        }
    }
}


