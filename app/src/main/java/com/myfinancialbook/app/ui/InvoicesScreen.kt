package com.myfinancialbook.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.myfinancialbook.app.R
import com.myfinancialbook.app.data.Invoice
import com.myfinancialbook.app.export.PdfExporter
import com.myfinancialbook.app.ui.theme.InvoicePurple
import com.myfinancialbook.app.ui.theme.LineColor
import com.myfinancialbook.app.ui.theme.Muted
import com.myfinancialbook.app.ui.theme.PrimaryBlue
import com.myfinancialbook.app.ui.theme.Red
import com.myfinancialbook.app.ui.theme.Surface
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun InvoicesScreen(vm: LedgerViewModel, onBack: () -> Unit) {
    val invoices by vm.invoices.collectAsState()
    val parties by vm.partiesWithBalance.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var invoiceToEdit by remember { mutableStateOf<Invoice?>(null) }
    var selectedInvoice by remember { mutableStateOf<Invoice?>(null) }
    var invoiceToDelete by remember { mutableStateOf<Invoice?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.invoices)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = InvoicePurple, contentColor = Surface) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        if (invoices.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No invoices generated yet.", color = Muted)
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(invoices) { inv ->
                    InvoiceRow(inv, parties.find { it.party.firestoreId == inv.partyId }?.party?.name ?: "Unknown", 
                        onClick = { selectedInvoice = inv },
                        onLongClick = { invoiceToEdit = inv }
                    )
                    HorizontalDivider(color = LineColor)
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditInvoiceDialog(
            parties = parties.map { it.party },
            onDismiss = { showAddDialog = false },
            onSave = { partyId, num, amt, items ->
                vm.addInvoice(partyId, amt, num, items)
                showAddDialog = false
            }
        )
    }

    invoiceToEdit?.let { inv ->
        AddEditInvoiceDialog(
            invoice = inv,
            parties = parties.map { it.party },
            onDismiss = { invoiceToEdit = null },
            onSave = { partyId, num, amt, items ->
                vm.updateInvoice(inv.copy(partyId = partyId, invoiceNumber = num, amount = amt, itemsJson = items))
                invoiceToEdit = null
            },
            onDelete = {
                invoiceToDelete = inv
                invoiceToEdit = null
            }
        )
    }

    selectedInvoice?.let { inv ->
        InvoicePreviewDialog(
            invoice = inv,
            partyName = parties.find { it.party.firestoreId == inv.partyId }?.party?.name ?: "Customer",
            businessName = vm.businessName.collectAsState().value,
            onDismiss = { selectedInvoice = null }
        )
    }

    invoiceToDelete?.let { inv ->
        AlertDialog(
            onDismissRequest = { invoiceToDelete = null },
            title = { Text("Delete Invoice") },
            text = { Text("Are you sure you want to delete invoice ${inv.invoiceNumber}?") },
            confirmButton = {
                TextButton(onClick = { vm.deleteInvoice(inv); invoiceToDelete = null }) {
                    Text("Delete", color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { invoiceToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun InvoiceRow(inv: Invoice, partyName: String, onClick: () -> Unit, onLongClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(inv.invoiceNumber, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(partyName, fontSize = 12.sp, color = Muted)
            Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(inv.date)), fontSize = 10.sp, color = Muted)
        }
        Text(money(inv.amount), fontWeight = FontWeight.Bold, color = InvoicePurple)
    }
}

data class InvoiceItem(val desc: String, val qty: Double = 1.0, val amt: Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditInvoiceDialog(
    invoice: Invoice? = null,
    parties: List<com.myfinancialbook.app.data.Party>, 
    onDismiss: () -> Unit, 
    onSave: (String, String, Double, String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var partyId by remember { mutableStateOf(invoice?.partyId ?: parties.firstOrNull()?.firestoreId ?: "") }
    var num by remember { mutableStateOf(invoice?.invoiceNumber ?: "INV-${System.currentTimeMillis().toString().takeLast(4)}") }
    
    val items = remember(invoice) { 
        mutableStateListOf<InvoiceItem>().apply {
            invoice?.itemsJson?.split(";")?.filter { it.contains("|") }?.forEach {
                val parts = it.split("|")
                val desc = parts[0]
                val qty = if (parts.size >= 3) parts[1].toDoubleOrNull() ?: 1.0 else 1.0
                val amt = if (parts.size >= 3) parts[2].toDoubleOrNull() else parts[1].toDoubleOrNull()
                if (desc.isNotBlank() && amt != null && amt > 0) {
                    add(InvoiceItem(desc, qty, amt))
                }
            }
        }
    }
    var currentDesc by remember { mutableStateOf("") }
    var currentQty by remember { mutableStateOf("1") }
    var currentUnitPrice by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (invoice == null) "Create Invoice" else "Edit Invoice") },
        text = {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding()
            ) {
                Text("Select Party", fontSize = 12.sp, color = Muted)
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(parties.find { it.firestoreId == partyId }?.name ?: "Select Party")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        parties.forEach { p ->
                            DropdownMenuItem(text = { Text(p.name) }, onClick = { partyId = p.firestoreId; expanded = false })
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = num, onValueChange = { num = it }, label = { Text("Invoice Number") }, modifier = Modifier.fillMaxWidth())
                
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Text("Items List", fontWeight = FontWeight.Bold)
                
                items.forEachIndexed { index, item ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${item.desc} × ${item.qty.toInt()} = ${money(item.amt)}", Modifier.weight(1f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = { items.removeAt(index) }) { Icon(Icons.Default.Delete, null, tint = Red, modifier = Modifier.size(20.dp)) }
                    }
                }
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = currentDesc, 
                        onValueChange = { currentDesc = it }, 
                        label = { Text("Item Name") }, 
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = currentQty, 
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 6) currentQty = it }, 
                        label = { Text("Qty") }, 
                        modifier = Modifier.weight(0.7f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = currentUnitPrice, 
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) currentUnitPrice = it }, 
                        label = { Text("Unit Price") }, 
                        modifier = Modifier.weight(1f),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        singleLine = true
                    )
                }
                TextButton(onClick = {
                    val qty = currentQty.toDoubleOrNull() ?: 1.0
                    val price = currentUnitPrice.toDoubleOrNull() ?: 0.0
                    val total = qty * price
                    if (currentDesc.isNotBlank() && total > 0) {
                        items.add(InvoiceItem(currentDesc.trim(), qty, total))
                        currentDesc = ""; currentQty = "1"; currentUnitPrice = ""
                        error = null
                    } else {
                        error = "Enter item name and unit price"
                    }
                }) {
                    Icon(Icons.Default.Add, null)
                    Text("Add Item to List")
                }

                error?.let { Text(it, color = Red, fontSize = 12.sp) }

                if (onDelete != null) {
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = Red), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Delete, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Delete Invoice")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // If there's an item in the input fields, add it automatically
                val qty = currentQty.toDoubleOrNull() ?: 1.0
                val price = currentUnitPrice.toDoubleOrNull() ?: 0.0
                if (currentDesc.isNotBlank() && price > 0) {
                    items.add(InvoiceItem(currentDesc.trim(), qty, qty * price))
                }
                
                val total = items.sumOf { it.amt }
                val itemsStr = items.joinToString(";") { "${it.desc}|${it.qty}|${it.amt}" }
                
                if (total > 0) {
                    onSave(partyId, num, total, itemsStr)
                } else {
                    error = "Please add at least one item with amount"
                }
            }) { Text(if (invoice == null) "Generate" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicePreviewDialog(invoice: Invoice, partyName: String, businessName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Invoice Preview") },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                    actions = {
                        IconButton(onClick = {
                            val file = PdfExporter.exportInvoice(context, businessName, partyName, invoice)
                            shareFile(context, file, "application/pdf")
                        }) {
                            Icon(Icons.Default.Download, null)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Surface)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(businessName.uppercase(), fontSize = 24.sp, fontWeight = FontWeight.Black, color = PrimaryBlue)
                Text("Tax Invoice", fontSize = 12.sp, color = Muted)
                
                Spacer(Modifier.height(24.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("BILL TO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Muted)
                        Text(partyName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("INVOICE #", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Muted)
                        Text(invoice.invoiceNumber, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text("DATE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Muted)
                        Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(invoice.date)), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                
                HorizontalDivider(Modifier.padding(vertical = 16.dp), thickness = 2.dp, color = MaterialTheme.colorScheme.onSurface)
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("DESCRIPTION", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                    Text("QTY", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                    Text("TOTAL", fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurface)
                }
                
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = LineColor)
                
                val items = invoice.itemsJson.split(";").filter { it.contains("|") }
                items.forEach { item ->
                    val parts = item.split("|")
                    val desc = parts[0]
                    val qty = if (parts.size >= 3) parts[1] else "1"
                    val amt = if (parts.size >= 3) parts[2] else parts[1]
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(desc, fontSize = 14.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                        Text(qty, fontSize = 14.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                        Text(money(amt.toDoubleOrNull() ?: 0.0), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                
                Spacer(Modifier.height(40.dp))
                
                Column(Modifier.align(Alignment.End)) {
                    Row(Modifier.width(200.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Subtotal", color = Muted)
                        Text(money(invoice.amount), color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(Modifier.width(200.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(money(invoice.amount), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryBlue)
                    }
                }
                
                Spacer(Modifier.height(64.dp))
                Text("This is a system-generated document and does not require a physical signature for validity.",
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Muted, fontSize = 10.sp)
                
                Spacer(Modifier.height(48.dp))
                Text("Thank you for your business!", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Muted, fontSize = 12.sp)
            }
        }
    }
}
