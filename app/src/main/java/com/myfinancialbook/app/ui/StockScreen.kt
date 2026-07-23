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
import com.myfinancialbook.app.data.StockItem
import com.myfinancialbook.app.ui.theme.LedgerGreen
import com.myfinancialbook.app.ui.theme.Surface
import com.myfinancialbook.app.ui.theme.LineColor
import com.myfinancialbook.app.ui.theme.Muted
import com.myfinancialbook.app.ui.theme.Red

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StockScreen(vm: LedgerViewModel, onBack: () -> Unit) {
    val stockItems by vm.stockItems.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<StockItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.stock)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = LedgerGreen, contentColor = Surface) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        if (stockItems.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No stock items yet.", color = Muted)
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(stockItems) { item ->
                    StockRow(item, onLongClick = { itemToDelete = item })
                    HorizontalDivider(color = LineColor)
                }
            }
        }
    }

    if (showAddDialog) {
        AddStockDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, qty, purchase, sale ->
                vm.addStockItem(name, qty, purchase, sale)
                showAddDialog = false
            }
        )
    }

    itemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Item") },
            text = { Text("Delete ${item.name} from stock?") },
            confirmButton = {
                TextButton(onClick = { vm.deleteStockItem(item); itemToDelete = null }) {
                    Text("Delete", color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StockRow(item: StockItem, onLongClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
            .padding(16.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("Qty: ${item.quantity} ${item.unit}", fontSize = 12.sp, color = Muted)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Sale: ${money(item.salePrice)}", fontWeight = FontWeight.SemiBold, color = LedgerGreen)
            Text("Purchase: ${money(item.purchasePrice)}", fontSize = 10.sp, color = Muted)
        }
    }
}

@Composable
fun AddStockDialog(onDismiss: () -> Unit, onSave: (String, Double, Double, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var purchase by remember { mutableStateOf("") }
    var sale by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Stock Item") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Quantity") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = purchase, onValueChange = { purchase = it }, label = { Text("Purchase Price") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = sale, onValueChange = { sale = it }, label = { Text("Sale Price") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(name, qty.toDoubleOrNull() ?: 0.0, purchase.toDoubleOrNull() ?: 0.0, sale.toDoubleOrNull() ?: 0.0)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
