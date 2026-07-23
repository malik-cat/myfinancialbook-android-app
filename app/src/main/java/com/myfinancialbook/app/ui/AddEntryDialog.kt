package com.myfinancialbook.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.myfinancialbook.app.ui.theme.LedgerGreen
import com.myfinancialbook.app.ui.theme.Red

@Composable
fun AddEntryDialog(type: String, onDismiss: () -> Unit, onSave: (Double, String, Long) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val color = if (type == "GIVE") Red else LedgerGreen
    val title = if (type == "GIVE") "You Gave (Payment Out)" else "You Got (Payment In)"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = Red, modifier = Modifier.padding(top = 6.dp)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amt = amount.toDoubleOrNull()
                if (amt == null || amt <= 0) { error = "Enter a valid amount"; return@TextButton }
                onSave(amt, note.trim(), System.currentTimeMillis())
            }) { Text("Save", color = color) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
