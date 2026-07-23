package com.myfinancialbook.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfinancialbook.app.R
import com.myfinancialbook.app.data.Party
import com.myfinancialbook.app.ui.theme.Background
import com.myfinancialbook.app.ui.theme.LedgerGreen
import com.myfinancialbook.app.ui.theme.LineColor
import com.myfinancialbook.app.ui.theme.Muted
import com.myfinancialbook.app.ui.theme.PrimaryBlue
import com.myfinancialbook.app.ui.theme.Red
import com.myfinancialbook.app.ui.theme.Surface
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PartiesScreen(vm: LedgerViewModel, onOpenParty: (String) -> Unit, onBack: () -> Unit) {
    val parties by vm.partiesWithBalance.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: All, 1: Customers, 2: Suppliers, 3: Banks
    var hideBalance by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val tabs = listOf("All", "Customers", "Suppliers", "Banks")
    
    val filteredParties = parties.filter { ui ->
        val matchesSearch = ui.party.name.contains(searchQuery, ignoreCase = true) || ui.party.phone.contains(searchQuery)
        val matchesTab = when(selectedTab) {
            1 -> ui.party.type == "CUSTOMER"
            2 -> ui.party.type == "SUPPLIER"
            3 -> ui.party.type == "BANK"
            else -> true
        }
        matchesSearch && matchesTab
    }

    val totalGet = filteredParties.filter { it.balance > 0 }.sumOf { it.balance }
    val totalGive = filteredParties.filter { it.balance < 0 }.sumOf { -it.balance }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.parties)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                actions = {
                    IconButton(onClick = { hideBalance = !hideBalance }) {
                        Icon(if (hideBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = LedgerGreen,
                contentColor = Surface,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(when(selectedTab) { 3 -> "ADD BANK"; 2 -> "ADD SUPPLIER"; else -> "ADD CUSTOMER" }) }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().background(Background)) {
            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = Surface,
                contentColor = PrimaryBlue,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Summary Card (Changes based on Tab)
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (selectedTab == 3) {
                    // Bank Specific Summary
                    Column(Modifier.padding(20.dp)) {
                        val currentMonth = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Total In ($currentMonth)", fontSize = 12.sp, color = Muted)
                                Text(money(totalGet), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = LedgerGreen)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Bank Balance", fontSize = 12.sp, color = Muted)
                                Text(money(totalGive - totalGet), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            }
                        }
                    }
                } else {
                    Row(Modifier.padding(20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("You will give", fontSize = 12.sp, color = Muted)
                            Text(
                                if (hideBalance) "Rs ••••" else money(totalGive),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = LedgerGreen
                            )
                        }
                        HorizontalDivider(Modifier.height(40.dp).width(1.dp), color = LineColor)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("You will get", fontSize = 12.sp, color = Muted)
                            Text(
                                if (hideBalance) "Rs ••••" else money(totalGet),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Red
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Search Party / Phone") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = Surface)
            )
            
            Spacer(Modifier.height(8.dp))

            if (filteredParties.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No parties found", color = Muted)
                }
            } else {
                LazyColumn {
                    items(filteredParties) { ui ->
                        PartyItem(ui, hideBalance) { onOpenParty(ui.party.firestoreId) }
                        HorizontalDivider(color = LineColor, thickness = 0.5.dp)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddPartyDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, phone, oType, amt, pType, note, img, voice ->
                vm.addParty(name, phone, oType, amt, pType, note, img, voice) { id ->
                    showAddDialog = false
                    onOpenParty(id)
                }
            }
        )
    }
}

@Composable
fun PartyItem(ui: PartyUi, hideBalance: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                ui.party.name.take(1).uppercase(),
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue
            )
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column(Modifier.weight(1f)) {
            Text(ui.party.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Last entry: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(ui.party.createdAt))}",
                fontSize = 11.sp,
                color = Muted
            )
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (hideBalance) "Rs ••••" else money(ui.balance),
                color = if (ui.balance >= 0) LedgerGreen else Red,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                if (ui.balance >= 0) "You'll Give" else "You'll Get",
                fontSize = 10.sp,
                color = Muted
            )
        }
    }
}
