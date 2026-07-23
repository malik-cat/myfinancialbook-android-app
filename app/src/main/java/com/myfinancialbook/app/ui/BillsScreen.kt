package com.myfinancialbook.app.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfinancialbook.app.R
import com.myfinancialbook.app.data.Bill
import com.myfinancialbook.app.ui.theme.BillsBlue
import com.myfinancialbook.app.ui.theme.LineColor
import com.myfinancialbook.app.ui.theme.Muted
import com.myfinancialbook.app.ui.theme.Red
import com.myfinancialbook.app.ui.theme.Surface
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BillsScreen(vm: LedgerViewModel, onBack: () -> Unit) {
    val bills by vm.bills.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var billToDelete by remember { mutableStateOf<Bill?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bills)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = BillsBlue, contentColor = Surface) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        if (bills.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No bills added yet.", color = Muted)
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(bills) { bill ->
                    BillRow(bill, onLongClick = { billToDelete = bill })
                    HorizontalDivider(color = LineColor)
                }
            }
        }
    }

    if (showAddDialog) {
        AddBillDialog(
            onDismiss = { showAddDialog = false },
            onSave = { num, prov, amt ->
                vm.addBill(num, prov, amt)
                showAddDialog = false
            }
        )
    }

    billToDelete?.let { bill ->
        AlertDialog(
            onDismissRequest = { billToDelete = null },
            title = { Text("Delete Bill") },
            text = { Text("Delete bill ${bill.billNumber} from ${bill.provider}?") },
            confirmButton = {
                TextButton(onClick = { vm.deleteBill(bill); billToDelete = null }) {
                    Text("Delete", color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { billToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BillRow(bill: Bill, onLongClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(bill.provider, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Bill #: ${bill.billNumber}", fontSize = 12.sp, color = Muted)
            Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(bill.date)), fontSize = 10.sp, color = Muted)
        }
        Text(money(bill.amount), fontWeight = FontWeight.Bold, color = BillsBlue)
    }
}

@Composable
fun AddBillDialog(onDismiss: () -> Unit, onSave: (String, String, Double) -> Unit) {
    var num by remember { mutableStateOf("") }
    var prov by remember { mutableStateOf("") }
    var amt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bill") },
        text = {
            Column {
                OutlinedTextField(value = prov, onValueChange = { prov = it }, label = { Text("Bill Provider") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = num, onValueChange = { num = it }, label = { Text("Bill Number") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amt, onValueChange = { amt = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(num, prov, amt.toDoubleOrNull() ?: 0.0)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
