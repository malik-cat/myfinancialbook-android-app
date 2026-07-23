package com.myfinancialbook.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfinancialbook.app.data.CashTransaction
import com.myfinancialbook.app.ui.theme.Green
import com.myfinancialbook.app.ui.theme.Muted
import com.myfinancialbook.app.ui.theme.Red
import com.myfinancialbook.app.ui.theme.Surface
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CashBookScreen(vm: LedgerViewModel, onBack: () -> Unit) {
    val transactions by vm.cashTransactions.collectAsState()
    val totalIn = transactions.filter { it.type == "IN" }.sumOf { it.amount }
    val totalOut = transactions.filter { it.type == "OUT" }.sumOf { it.amount }
    val balance = totalIn - totalOut
    
    var showAddDialog by remember { mutableStateOf<String?>(null) } // "IN" or "OUT"
    var txToDelete by remember { mutableStateOf<CashTransaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cash Book") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CashStat("Cash In", totalIn, Green)
                CashStat("Cash Out", totalOut, Red)
                CashStat("Balance", balance, if (balance >= 0) Green else Red)
            }
            
                    HorizontalDivider()

            LazyColumn(Modifier.weight(1f)) {
                items(transactions) { tx ->
                    CashRow(tx, onLongClick = { txToDelete = tx })
            HorizontalDivider()
                }
            }

            Row(Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = { showAddDialog = "OUT" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                ) {
                    Text("Out (-)")
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = { showAddDialog = "IN" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Green)
                ) {
                    Text("In (+)")
                }
            }
        }
    }

    showAddDialog?.let { type ->
        AddCashDialog(
            type = type,
            onDismiss = { showAddDialog = null },
            onSave = { amount, note ->
                vm.addCashTransaction(amount, type, note)
                showAddDialog = null
            }
        )
    }

    txToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text("Delete Transaction") },
            text = { Text("Delete cash entry of ${money(tx.amount)}?") },
            confirmButton = {
                TextButton(onClick = { vm.deleteCashTransaction(tx); txToDelete = null }) {
                    Text("Delete", color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun CashStat(label: String, amount: Double, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = Muted)
        Text(money(amount), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun CashRow(tx: CashTransaction, onLongClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(tx.note.ifBlank { if (tx.type == "IN") "Cash In" else "Cash Out" }, fontWeight = FontWeight.Medium)
            Text(SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp)), fontSize = 12.sp, color = Muted)
        }
        Text(
            money(tx.amount),
            color = if (tx.type == "IN") Green else Red,
            fontWeight = FontWeight.Bold
        )
    }
}
