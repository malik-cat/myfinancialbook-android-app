package com.myfinancialbook.app.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.myfinancialbook.app.data.LedgerEntry
import com.myfinancialbook.app.ui.theme.CalculatorBg
import com.myfinancialbook.app.ui.theme.LedgerGreen
import com.myfinancialbook.app.ui.theme.Muted
import com.myfinancialbook.app.ui.theme.PrimaryBlue
import com.myfinancialbook.app.ui.theme.Red
import com.myfinancialbook.app.ui.theme.Surface
import com.myfinancialbook.app.util.FileUtil
import com.myfinancialbook.app.util.VoicePlayer
import com.myfinancialbook.app.util.VoiceRecorder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(vm: LedgerViewModel, partyId: String, type: String, entryId: Long = 0L, onBack: () -> Unit) {
    val context = LocalContext.current
    val voiceRecorder = remember { VoiceRecorder(context) }
    
    val existingEntry by produceState<LedgerEntry?>(initialValue = if (entryId != 0L) vm.getEntrySync(entryId) else null, entryId) {
        if (entryId != 0L) {
            // Poll until the entry is available
            repeat(20) {
                val found = vm.getEntrySync(entryId)
                if (found != null) {
                    value = found
                    return@produceState
                }
                kotlinx.coroutines.delay(50)
            }
        }
    }
    
    var amount by remember { mutableStateOf(existingEntry?.amount?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var note by remember { mutableStateOf(existingEntry?.note ?: "") }
    
    val calendar = remember { 
        Calendar.getInstance().apply { 
            existingEntry?.timestamp?.let { timeInMillis = it }
        } 
    }
    var timestamp by remember { mutableLongStateOf(existingEntry?.timestamp ?: System.currentTimeMillis()) }
    
    var attachmentUri by remember { mutableStateOf<Uri?>(existingEntry?.attachmentPath?.let { Uri.parse(it) }) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var voiceNotePath by remember { mutableStateOf<String?>(existingEntry?.voiceNotePath) }
    var isRecording by remember { mutableStateOf(false) }
    var isCash by remember { mutableStateOf(false) }
    var showSourcePicker by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission is required for voice notes", Toast.LENGTH_SHORT).show()
        }
    }
    
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> attachmentUri = uri }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> if (success) attachmentUri = tempCameraUri }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> 
        if (isGranted) {
            val file = File(context.cacheDir, "temp_entry_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    DisposableEffect(Unit) {
        onDispose { voiceRecorder.stopRecording() }
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            timestamp = calendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            timestamp = calendar.timeInMillis
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (entryId != 0L) "Edit Entry" else (if (type == "GET") "You Got (In)" else "You Gave (Out)"), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (type == "GET") LedgerGreen else Red, 
                    titleContentColor = Color.White, 
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Surface)
        ) {
            // Form area - Scrollable
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Amount Input
                Text("Enter Amount", fontSize = 12.sp, color = Muted, fontWeight = FontWeight.Bold)
                Text(
                    text = "PKR ${amount.ifBlank { "0" }}",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = if (type == "GET") LedgerGreen else Red,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Spacer(Modifier.height(16.dp))
                
                // Form Fields
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Entry Note (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (voiceRecorder.isRecording) {
                                    voiceNotePath = voiceRecorder.stopRecording()
                                    isRecording = false
                                    if (voiceNotePath != null) {
                                        Toast.makeText(context, "Voice note saved", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        if (voiceRecorder.startRecording()) {
                                            isRecording = true
                                        }
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                if (isRecording) Icons.Default.StopCircle else Icons.Default.Mic, 
                                null, 
                                tint = if (isRecording) Red else PrimaryBlue, 
                            )
                        }
                    }
                )

                if (isRecording) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.FiberManualRecord, null, tint = Red, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Recording... Tap mic again to stop", color = Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (voiceNotePath != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        TextButton(onClick = { 
                            try {
                                VoicePlayer.play(context, voiceNotePath!!)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error playing audio", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.PlayCircle, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Listen Recorded Voice")
                        }
                        IconButton(onClick = { voiceNotePath = null }) {
                            Icon(Icons.Default.Delete, null, tint = Red, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { datePickerDialog.show() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp)), fontSize = 11.sp, maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = { timePickerDialog.show() },
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Schedule, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp)), fontSize = 11.sp, maxLines = 1)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Column {
                    OutlinedButton(
                        onClick = { showSourcePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = if (attachmentUri != null) LedgerGreen else PrimaryBlue)
                    ) {
                        Icon(if (attachmentUri != null) Icons.Default.CheckCircle else Icons.Default.CameraAlt, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (attachmentUri != null) "Image Attached" else "Snap or Attach Image")
                    }
                    
                    if (attachmentUri != null) {
                        Box(Modifier.padding(top = 8.dp)) {
                            AsyncImage(
                                model = attachmentUri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CalculatorBg)
                            )
                            IconButton(
                                onClick = { attachmentUri = null },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                
                if (entryId == 0L) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Add to Cash Book?", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Switch(checked = isCash, onCheckedChange = { isCash = it }, colors = SwitchDefaults.colors(checkedThumbColor = PrimaryBlue))
                    }
                }
            }
            
            // Calculator Grid - Fixed at bottom
            CalculatorGrid(
                onDigit = { digit -> if (amount.length < 10) amount += digit },
                onOperator = { /* math */ },
                onClear = { amount = "" },
                onDelete = { if (amount.isNotEmpty()) amount = amount.dropLast(1) },
                onSave = {
                    val amtVal = amount.toDoubleOrNull() ?: 0.0
                    if (amtVal > 0) {
                        val finalImagePath = attachmentUri?.let { uri ->
                            if (uri.toString().startsWith("content")) {
                                FileUtil.copyUriToInternal(context, uri, "entry_img")
                            } else {
                                uri.toString()
                            }
                        }
                        
                        val currentEntry = existingEntry
                        if (currentEntry != null) {
                            vm.updateEntry(currentEntry.copy(
                                amount = amtVal,
                                note = note,
                                timestamp = timestamp,
                                attachmentPath = finalImagePath,
                                voiceNotePath = voiceNotePath
                            ))
                        } else {
                            vm.addEntry(
                                partyId = partyId,
                                type = type,
                                amount = amtVal,
                                note = note,
                                timestamp = timestamp,
                                attachmentPath = finalImagePath,
                                voiceNotePath = voiceNotePath
                            )
                            if (isCash) {
                                vm.addCashTransaction(amtVal, if (type == "GET") "IN" else "OUT", "Ledger: $note")
                            }
                        }
                        onBack()
                    } else {
                        Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    if (showSourcePicker) {
        AlertDialog(
            onDismissRequest = { showSourcePicker = false },
            title = { Text("Select Image Source") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Camera") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, null) },
                        modifier = Modifier.clickable {
                            showSourcePicker = false
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                val file = File(context.cacheDir, "temp_entry_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                tempCameraUri = uri
                                cameraLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Gallery") },
                        leadingContent = { Icon(Icons.Default.PhotoLibrary, null) },
                        modifier = Modifier.clickable {
                            showSourcePicker = false
                            galleryPicker.launch("image/*")
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun CalculatorGrid(
    onDigit: (String) -> Unit,
    onOperator: (String) -> Unit,
    onClear: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CalculatorBg)
            .padding(4.dp)
    ) {
        val rows = listOf(
            listOf("7", "8", "9", "÷"),
            listOf("4", "5", "6", "×"),
            listOf("1", "2", "3", "-"),
            listOf(".", "0", "AC", "+")
        )
        
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { char ->
                    CalcButton(char, Modifier.weight(1f)) {
                        when (char) {
                            "AC" -> onClear()
                            "÷", "×", "-", "+" -> onOperator(char)
                            else -> onDigit(char)
                        }
                    }
                }
            }
        }
        
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            CalcButton("DEL", Modifier.weight(1f), CalculatorBg) { onDelete() }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(2f).height(54.dp).padding(4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("SAVE ENTRY", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun CalcButton(label: String, modifier: Modifier = Modifier, color: Color = Surface, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .padding(2.dp)
            .height(54.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = color,
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
