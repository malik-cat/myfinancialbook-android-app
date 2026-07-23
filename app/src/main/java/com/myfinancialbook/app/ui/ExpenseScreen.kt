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
import com.myfinancialbook.app.data.Expense
import com.myfinancialbook.app.ui.theme.Red
import com.myfinancialbook.app.ui.theme.Surface
import com.myfinancialbook.app.ui.theme.LineColor
import com.myfinancialbook.app.ui.theme.Muted
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ExpenseScreen(vm: LedgerViewModel, onBack: () -> Unit) {
    val expenses by vm.expenses.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    val total = expenses.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.expense)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = Red, contentColor = Surface) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Card(
                Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Red.copy(alpha = 0.1f))
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Expenses", fontSize = 14.sp, color = Muted)
                    Text(money(total), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Red)
                }
            }

            if (expenses.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No expenses recorded.", color = Muted)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(expenses) { exp ->
                        ExpenseRow(exp, onLongClick = { expenseToDelete = exp })
                        HorizontalDivider(color = LineColor)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddExpenseDialog(
            onDismiss = { showAddDialog = false },
            onSave = { amount, category, note ->
                vm.addExpense(amount, category, note)
                showAddDialog = false
            }
        )
    }

    expenseToDelete?.let { exp ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = { Text("Delete Expense") },
            text = { Text("Delete expense for ${exp.category}: ${money(exp.amount)}?") },
            confirmButton = {
                TextButton(onClick = { vm.deleteExpense(exp); expenseToDelete = null }) {
                    Text("Delete", color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ExpenseRow(exp: Expense, onLongClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
            .padding(16.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(exp.category, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(exp.note.ifBlank { "No note" }, fontSize = 12.sp, color = Muted)
            Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(exp.timestamp)), fontSize = 10.sp, color = Muted)
        }
        Text(money(exp.amount), fontWeight = FontWeight.Bold, color = Red)
    }
}

@Composable
fun AddExpenseDialog(onDismiss: () -> Unit, onSave: (Double, String, String) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Expense") },
        text = {
            Column {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (e.g. Rent, Salary)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(amount.toDoubleOrNull() ?: 0.0, category.ifBlank { "General" }, note)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
