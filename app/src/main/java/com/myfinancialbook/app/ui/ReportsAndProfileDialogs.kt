package com.myfinancialbook.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfinancialbook.app.R
import com.myfinancialbook.app.export.PdfExporter
import com.myfinancialbook.app.export.XlsxExporter
import com.myfinancialbook.app.ui.theme.AccentTeal
import com.myfinancialbook.app.ui.theme.LedgerGreen
import com.myfinancialbook.app.ui.theme.Red
import com.myfinancialbook.app.util.SecurityManager

@Composable
fun ProfileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onLanguageChange: (String) -> Unit = {},
    onToggleBiometric: (Boolean) -> Unit = {},
    onPinChange: (String) -> Unit = {},
    onRestore: () -> Unit = {},
    onSyncFromCloud: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(currentName) }
    var biometricEnabled by remember { mutableStateOf(SecurityManager.isBiometricEnabled(context)) }
    var pin by remember { mutableStateOf(SecurityManager.getBackupPin(context) ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_profile)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                    // Restore Section - High Visibility
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AccentTeal.copy(alpha = 0.1f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CloudDownload, null, tint = AccentTeal)
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.need_old_data), fontWeight = FontWeight.Bold)
                            Button(
                                onClick = onRestore,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
                            ) {
                                Text(stringResource(R.string.restore_from_drive))
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = onSyncFromCloud,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.sync_cloud))
                            }
                        }
                    }

                HorizontalDivider(Modifier.padding(vertical = 12.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.business_name)) }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.enable_fingerprint), modifier = Modifier.weight(1f))
                    Switch(checked = biometricEnabled, onCheckedChange = { 
                        biometricEnabled = it
                        onToggleBiometric(it)
                    })
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) { pin = it; onPinChange(it) } },
                    label = { Text(stringResource(R.string.app_pin)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                )

                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.change_language), fontWeight = FontWeight.Bold)
                Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { onLanguageChange("en") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.english), fontSize = 12.sp) }
                        Button(onClick = { onLanguageChange("ur") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.urdu), fontSize = 12.sp) }
                        Button(onClick = { onLanguageChange("ar") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.arabic), fontSize = 12.sp) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { onLanguageChange("hi") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.hindi), fontSize = 12.sp) }
                        Button(onClick = { onLanguageChange("tr") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.turkish), fontSize = 12.sp) }
                        Button(onClick = { onLanguageChange("es") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.spanish), fontSize = 12.sp) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(onClick = { onLanguageChange("pt") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.portuguese), fontSize = 12.sp) }
                        Button(onClick = { onLanguageChange("fr") }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.french), fontSize = 12.sp) }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 12.dp))

                // Logout Section
                val currentUserEmail = com.myfinancialbook.app.util.AuthManager.getUserEmail()
                if (currentUserEmail.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Icon(Icons.Default.Email, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(currentUserEmail, fontSize = 13.sp, color = Color.Gray)
                    }
                }
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Logout, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.sign_out), fontWeight = FontWeight.SemiBold)
                }

            }
        },
        confirmButton = { TextButton(onClick = { onSave(name.ifBlank { "My Business" }) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun ReportsDialog(vm: LedgerViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val businessName by vm.businessName.collectAsState()
    val parties by vm.partiesWithBalance.collectAsState()
    val entriesMap by vm.entriesByPartyId.collectAsState()
    val totalGet = parties.filter { it.balance > 0 }.sumOf { it.balance }
    val totalGive = parties.filter { it.balance < 0 }.sumOf { -it.balance }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reports)) },
        text = {
            Column {
                Text("Download your complete ledger across all parties.")
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total you will get")
                    Text(money(totalGet), color = LedgerGreen)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total you will give")
                    Text(money(totalGive), color = Red)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val file = PdfExporter.exportAll(context, businessName, parties.map { it.party }, entriesMap)
                shareFile(context, file, "application/pdf")
                onDismiss()
            }) { Text("PDF (All)") }
        },
        dismissButton = {
            TextButton(onClick = {
                val file = XlsxExporter.exportAll(context, parties.map { it.party }, entriesMap)
                shareFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                onDismiss()
            }) { Text("Excel (All)") }
        }
    )
}
