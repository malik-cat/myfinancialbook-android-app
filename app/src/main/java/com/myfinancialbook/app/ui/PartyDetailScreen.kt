package com.myfinancialbook.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.myfinancialbook.app.data.LedgerEntry
import com.myfinancialbook.app.data.balanceOf
import com.myfinancialbook.app.export.PdfExporter
import com.myfinancialbook.app.ui.theme.AccentTeal
import com.myfinancialbook.app.ui.theme.Background
import com.myfinancialbook.app.ui.theme.LedgerGreen
import com.myfinancialbook.app.ui.theme.LineColor
import com.myfinancialbook.app.ui.theme.Muted
import com.myfinancialbook.app.ui.theme.PrimaryBlue
import com.myfinancialbook.app.ui.theme.Red
import com.myfinancialbook.app.ui.theme.Surface
import com.myfinancialbook.app.ui.theme.TealLight
import com.myfinancialbook.app.util.VoicePlayer
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PartyDetailScreen(vm: LedgerViewModel, partyId: String, onBack: () -> Unit, onAddEntry: (String) -> Unit, onEditEntry: (Long) -> Unit) {
    val context = LocalContext.current
    val businessName by vm.businessName.collectAsState()
    val entriesMap by vm.entriesByPartyId.collectAsState()
    val partiesUi by vm.partiesWithBalance.collectAsState()
    val partyUi = partiesUi.find { it.party.firestoreId == partyId }
    val entries = entriesMap[partyId] ?: emptyList()
    var selectedEntry by remember { mutableStateOf<LedgerEntry?>(null) }
    
    var searchQuery by remember { mutableStateOf("") }
    var sortByRecent by remember { mutableStateOf(true) }
    var filterType by remember { mutableStateOf("ALL") } // ALL, TODAY, MONTH

    if (partyUi == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val party = partyUi.party
    val bal = balanceOf(entries)
    
    val filteredEntries = entries.filter { 
        val matchesSearch = it.note.contains(searchQuery, ignoreCase = true) || it.amount.toString().contains(searchQuery)
        val matchesFilter = when(filterType) {
            "TODAY" -> {
                val calNow = Calendar.getInstance()
                val calEntry = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                calNow.get(Calendar.DAY_OF_YEAR) == calEntry.get(Calendar.DAY_OF_YEAR) && 
                calNow.get(Calendar.YEAR) == calEntry.get(Calendar.YEAR)
            }
            "MONTH" -> {
                val calNow = Calendar.getInstance()
                val calEntry = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                calNow.get(Calendar.MONTH) == calEntry.get(Calendar.MONTH) && 
                calNow.get(Calendar.YEAR) == calEntry.get(Calendar.YEAR)
            }
            else -> true
        }
        matchesSearch && matchesFilter
    }.let {
        if (sortByRecent) it.sortedByDescending { e -> e.timestamp }
        else it.sortedBy { e -> e.timestamp }
    }

    val dateFmt = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    val runningById = remember(entries) {
        var running = 0.0
        entries.sortedBy { it.timestamp }.associate { e ->
            running += if (e.type == "GET") e.amount else -e.amount
            e.id to running
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                            Text(party.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(party.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(party.phone.ifBlank { "No phone" }, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                },
                navigationIcon = { 
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } 
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onAddEntry("GIVE") }, 
                        colors = ButtonDefaults.buttonColors(containerColor = Red), 
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("YOU GAVE", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onAddEntry("GET") }, 
                        colors = ButtonDefaults.buttonColors(containerColor = LedgerGreen), 
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("YOU GOT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().background(Background)) {
            // Summary Header
            Card(
                Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(money(bal), fontSize = 28.sp, fontWeight = FontWeight.Black, color = if (bal >= 0) LedgerGreen else Red)
                    Text(if (bal >= 0) "YOU WILL GIVE" else "YOU WILL GET", fontSize = 11.sp, color = Muted, letterSpacing = 1.sp)
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ActionIcon(Icons.Outlined.PictureAsPdf, "PDF") {
                            val file = PdfExporter.exportParty(context, businessName, party, entries)
                            shareFile(context, file, "application/pdf")
                        }
                        
                        var showFilterMenu by remember { mutableStateOf(false) }
                        Box {
                            ActionIcon(Icons.Outlined.CalendarMonth, "Filter") {
                                showFilterMenu = true
                            }
                            DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                                DropdownMenuItem(text = { Text("All Time") }, onClick = { filterType = "ALL"; showFilterMenu = false })
                                DropdownMenuItem(text = { Text("Today") }, onClick = { filterType = "TODAY"; showFilterMenu = false })
                                DropdownMenuItem(text = { Text("This Month") }, onClick = { filterType = "MONTH"; showFilterMenu = false })
                            }
                        }

                        ActionIcon(Icons.Outlined.NotificationsActive, "Reminder") {
                            Toast.makeText(context, "Reminder set for ${party.name}", Toast.LENGTH_SHORT).show()
                        }
                        ActionIcon(Icons.Outlined.Sms, "SMS") {
                            val msg = "Hello ${party.name}, your balance is ${money(bal)} in My Financial Book."
                            val uri = android.net.Uri.parse("smsto:${party.phone}")
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO, uri).apply {
                                putExtra("sms_body", msg)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No SMS app found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

            // Search & Sort Bar
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search entries...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(18.dp)) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { sortByRecent = !sortByRecent }) {
                    Icon(if (sortByRecent) Icons.Default.Sort else Icons.Default.LowPriority, null, tint = PrimaryBlue)
                }
            }

            // Ledger Entries
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("ENTRIES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Muted)
                Row {
                    Text("YOU GAVE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Red, modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
                    Spacer(Modifier.width(16.dp))
                    Text("YOU GOT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LedgerGreen, modifier = Modifier.width(80.dp), textAlign = TextAlign.End)
                }
            }
            HorizontalDivider(color = LineColor)

            if (filteredEntries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (searchQuery.isEmpty()) "No entries yet." else "No matching entries found.", color = Muted)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(filteredEntries) { e ->
                        EntryRowDetailed(e, dateFmt.format(Date(e.timestamp)), runningById[e.id] ?: 0.0) {
                            selectedEntry = e
                        }
                        HorizontalDivider(color = LineColor, thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        EntryDetailDialog(
            entry = entry,
            partyName = party.name,
            onDismiss = { selectedEntry = null },
            onDelete = { vm.deleteEntry(entry); selectedEntry = null },
            onEdit = { 
                selectedEntry = null
                onEditEntry(entry.id) 
            }
        )
    }
}

@Composable
fun ActionIcon(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 10.sp, color = Muted, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun EntryRowDetailed(e: LedgerEntry, dateStr: String, running: Double, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(dateStr, fontSize = 10.sp, color = Muted)
            Text(e.note.ifBlank { "No remarks" }, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("Bal. ${money(running)}", fontSize = 10.sp, color = LedgerGreen.copy(alpha = 0.8f))
            if (e.attachmentPath != null || e.voiceNotePath != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    if (e.attachmentPath != null) {
                        Icon(Icons.Default.AttachFile, null, Modifier.size(12.dp), tint = Muted)
                        Spacer(Modifier.width(4.dp))
                    }
                    if (e.voiceNotePath != null) {
                        Icon(Icons.Default.Mic, null, Modifier.size(12.dp), tint = Muted)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text("Media Attached", fontSize = 9.sp, color = Muted)
                }
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(80.dp), contentAlignment = Alignment.CenterEnd) {
                if (e.type == "GIVE") {
                    Text(money(e.amount), color = Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.width(16.dp))
            Box(Modifier.width(80.dp), contentAlignment = Alignment.CenterEnd) {
                if (e.type == "GET") {
                    Text(money(e.amount), color = LedgerGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailDialog(entry: LedgerEntry, partyName: String, onDismiss: () -> Unit, onDelete: () -> Unit, onEdit: () -> Unit) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Entry Details") },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                    actions = {
                        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Red) }
                    }
                )
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize().background(Surface).padding(24.dp)) {
                Text(
                    if (entry.type == "GET") "You Got ${money(entry.amount)}\nFrom $partyName" 
                    else "You Gave ${money(entry.amount)}\nTo $partyName",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (entry.type == "GET") LedgerGreen else Red,
                    lineHeight = 28.sp
                )
                
                Spacer(Modifier.height(24.dp))
                
                DetailRow("Date & Time", SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(entry.timestamp)))
                DetailRow("Description", entry.note.ifBlank { "No remarks added" })
                
                Spacer(Modifier.height(32.dp))
                
                Text("MEDIA & ATTACHMENTS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Muted)
                
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (entry.attachmentPath != null) {
                        Box(
                            Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(LineColor),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = File(entry.attachmentPath),
                                contentDescription = "Attachment",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    
                    if (entry.voiceNotePath != null) {
                        Surface(
                            onClick = { 
                                try {
                                    VoicePlayer.play(context, entry.voiceNotePath)
                                    Toast.makeText(context, "Playing voice note...", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error playing audio", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = TealLight,
                            modifier = Modifier.height(100.dp).weight(1f)
                        ) {
                            Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.PlayArrow, null, tint = AccentTeal)
                                Text("Play Voice", fontSize = 12.sp, color = AccentTeal, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                if (entry.attachmentPath == null && entry.voiceNotePath == null) {
                    Text("No attachments", fontSize = 13.sp, color = Muted, modifier = Modifier.padding(top = 8.dp))
                }
                
                Spacer(Modifier.weight(1f))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Edit, null)
                        Spacer(Modifier.width(8.dp))
                        Text("EDIT")
                    }
                    Button(
                        onClick = {
                            val shareMsg = "Entry from My Financial Book: $partyName ${if (entry.type == "GET") "paid" else "received"} ${money(entry.amount)} on ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(entry.timestamp))}"
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, shareMsg)
                                setPackage("com.whatsapp")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback to normal share if WhatsApp is not there
                                val fallback = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, shareMsg)
                                }
                                context.startActivity(android.content.Intent.createChooser(fallback, "Share via"))
                            }
                        }, 
                        modifier = Modifier.weight(1f), 
                        shape = RoundedCornerShape(12.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = LedgerGreen)
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text("WHATSAPP")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Column(Modifier.padding(vertical = 12.dp)) {
        Text(label, fontSize = 11.sp, color = Muted)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        HorizontalDivider(Modifier.padding(top = 12.dp), color = LineColor, thickness = 0.5.dp)
    }
}

fun shareFile(context: android.content.Context, file: File, mime: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = mime
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share statement"))
}
