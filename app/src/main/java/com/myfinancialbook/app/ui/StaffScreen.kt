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
import com.myfinancialbook.app.data.Staff
import com.myfinancialbook.app.ui.theme.LineColor
import com.myfinancialbook.app.ui.theme.Muted
import com.myfinancialbook.app.ui.theme.Red
import com.myfinancialbook.app.ui.theme.StaffOrange
import com.myfinancialbook.app.ui.theme.Surface

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StaffScreen(vm: LedgerViewModel, onBack: () -> Unit) {
    val staffList by vm.staff.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var staffToDelete by remember { mutableStateOf<Staff?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.staff)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }, containerColor = StaffOrange, contentColor = Surface) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        if (staffList.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No staff added yet.", color = Muted)
            }
        } else {
            LazyColumn(Modifier.padding(padding).fillMaxSize()) {
                items(staffList) { staff ->
                    StaffRow(staff, onLongClick = { staffToDelete = staff })
                    HorizontalDivider(color = LineColor)
                }
            }
        }
    }

    if (showAddDialog) {
        AddStaffDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, phone, sal ->
                vm.addStaff(name, phone, sal)
                showAddDialog = false
            }
        )
    }

    staffToDelete?.let { staff ->
        AlertDialog(
            onDismissRequest = { staffToDelete = null },
            title = { Text("Delete Staff") },
            text = { Text("Are you sure you want to delete ${staff.name}?") },
            confirmButton = {
                TextButton(onClick = { vm.deleteStaff(staff); staffToDelete = null }) {
                    Text("Delete", color = Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { staffToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun StaffRow(staff: Staff, onLongClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(staff.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(staff.phone.ifBlank { "No phone" }, fontSize = 12.sp, color = Muted)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(money(staff.salary), fontWeight = FontWeight.Bold, color = StaffOrange)
            Text("Salary", fontSize = 10.sp, color = Muted)
        }
    }
}

@Composable
fun AddStaffDialog(onDismiss: () -> Unit, onSave: (String, String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var sal by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Staff") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Staff Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = sal, onValueChange = { sal = it }, label = { Text("Salary") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(name, phone, sal.toDoubleOrNull() ?: 0.0)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
