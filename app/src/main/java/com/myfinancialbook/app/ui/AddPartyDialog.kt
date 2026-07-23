package com.myfinancialbook.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.myfinancialbook.app.ui.theme.LedgerGreen
import com.myfinancialbook.app.ui.theme.Muted
import com.myfinancialbook.app.ui.theme.PrimaryBlue
import com.myfinancialbook.app.ui.theme.Red
import com.myfinancialbook.app.ui.theme.Surface
import com.myfinancialbook.app.util.VoiceRecorder
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPartyDialog(onDismiss: () -> Unit, onSave: (String, String, String?, Double, String, String, String?, String?) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var opening by remember { mutableStateOf("") }
    var oType by remember { mutableStateOf("GET") }
    var partyType by remember { mutableStateOf("CUSTOMER") }
    var note by remember { mutableStateOf("") }
    var attachmentUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var voiceNotePath by remember { mutableStateOf<String?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showSourcePicker by remember { mutableStateOf(false) }
    val voiceRecorder = remember { VoiceRecorder(context) }
    
    DisposableEffect(Unit) {
        onDispose { voiceRecorder.stopRecording() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Microphone permission is required for voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let { contactUri ->
            try {
                val contentResolver = context.contentResolver
                val cursor = contentResolver.query(contactUri, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                        val pickedName = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME))
                        name = pickedName
                        
                        val hasPhone = c.getInt(c.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                        if (hasPhone > 0) {
                            val pCursor = contentResolver.query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                null,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                arrayOf(id),
                                null
                            )
                            pCursor?.use { pc ->
                                if (pc.moveToFirst()) {
                                    val number = pc.getString(pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                                    phone = number.replace("\\s".toRegex(), "")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> attachmentUri = uri }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> if (success) attachmentUri = tempCameraUri }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> if (isGranted) contactPickerLauncher.launch(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> 
        if (isGranted) {
            val file = File(context.cacheDir, "temp_party_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Party") },
        text = {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .imePadding() // Ensures keyboard doesn't push the WHOLE UI, just this content
            ) {
                Text("Category", fontSize = 12.sp, color = Muted)
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = partyType == "CUSTOMER", onClick = { partyType = "CUSTOMER" }, label = { Text("Customer") })
                    FilterChip(selected = partyType == "SUPPLIER", onClick = { partyType = "SUPPLIER" }, label = { Text("Supplier") })
                    FilterChip(selected = partyType == "BANK", onClick = { partyType = "BANK" }, label = { Text("Bank") })
                }

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                
                Spacer(Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                            contactPickerLauncher.launch(null)
                        } else {
                            contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    }) { Icon(Icons.Default.ContactPage, "Pick Contact", tint = LedgerGreen) }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                Text("Opening Balance Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                OutlinedTextField(
                    value = opening, 
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) opening = it }, 
                    label = { Text("Opening Amount") }, 
                    singleLine = true, 
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
                
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    FilterChip(selected = oType == "GET", onClick = { oType = "GET" }, label = { Text("They owe you") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LedgerGreen, selectedLabelColor = Surface))
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = oType == "GIVE", onClick = { oType = "GIVE" }, label = { Text("You owe them") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Red, selectedLabelColor = Surface))
                }

                Spacer(Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = note, 
                    onValueChange = { note = it }, 
                    label = { Text("Add Note/Remarks") }, 
                    modifier = Modifier.fillMaxWidth(),
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
                                tint = if (isRecording) Red else PrimaryBlue
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

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showSourcePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(if (attachmentUri != null) Icons.Default.CheckCircle else Icons.Default.AddAPhoto, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (attachmentUri != null) "Image Attached" else "Snap or Attach Image")
                }

                error?.let { Text(it, color = Red, modifier = Modifier.padding(top = 6.dp)) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) { error = "Please enter a name"; return@TextButton }
                val amt = opening.toDoubleOrNull() ?: 0.0
                onSave(name.trim(), phone.trim(), if (amt > 0) oType else null, amt, partyType, note, attachmentUri?.toString(), voiceNotePath)
            }) { Text("SAVE") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )

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
                                val file = File(context.cacheDir, "temp_party_${System.currentTimeMillis()}.jpg")
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
