package com.myfinancialbook.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myfinancialbook.app.R
import com.myfinancialbook.app.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale

fun money(v: Double): String = "PKR " + String.format(Locale.getDefault(), "%,.0f", Math.abs(v))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: LedgerViewModel,
    onOpenParty: (String) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenCashBook: () -> Unit = {},
    onOpenParties: () -> Unit = {},
    onOpenStock: () -> Unit = {},
    onOpenExpenses: () -> Unit = {},
    onOpenBills: () -> Unit = {},
    onOpenStaff: () -> Unit = {},
    onOpenInvoices: () -> Unit = {},
    onSync: () -> Unit = {},
    onRestore: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val businessName by vm.businessName.collectAsState()
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scrollState = rememberScrollState()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = PrimaryBlue) {
                Spacer(Modifier.height(24.dp))
                Text(stringResource(R.string.financial_book), modifier = Modifier.padding(24.dp), color = Surface, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = 2.sp)
                HorizontalDivider(color = Surface.copy(alpha = 0.1f))

                val items = listOf(
                    Triple(stringResource(R.string.home), Icons.Default.Dashboard, "home"),
                    Triple(stringResource(R.string.parties), Icons.Default.Groups, "parties"),
                    Triple(stringResource(R.string.cash_book), Icons.Default.AccountBalanceWallet, "cash"),
                    Triple(stringResource(R.string.stock), Icons.Default.Inventory2, "stock"),
                    Triple(stringResource(R.string.invoices), Icons.Default.Description, "invoices"),
                    Triple(stringResource(R.string.bills), Icons.Default.ReceiptLong, "bills"),
                    Triple(stringResource(R.string.staff), Icons.Default.Badge, "staff")
                )

                items.forEach { (label, icon, route) ->
                    NavigationDrawerItem(
                        label = { Text(label, color = Surface) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            when(route) {
                                "parties" -> onOpenParties()
                                "cash" -> onOpenCashBook()
                                "stock" -> onOpenStock()
                                "invoices" -> onOpenInvoices()
                                "bills" -> onOpenBills()
                                "staff" -> onOpenStaff()
                            }
                        },
                        icon = { Icon(icon, null, tint = AccentTeal) },
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                    )
                }

                HorizontalDivider(color = Surface.copy(alpha = 0.1f))
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.restore_data), color = Surface) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onRestore() },
                    icon = { Icon(Icons.Default.CloudDownload, null, tint = AccentTeal) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                Spacer(Modifier.weight(1f))
                HorizontalDivider(color = Surface.copy(alpha = 0.1f))
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.settings_profile), color = Surface) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onOpenProfile() },
                    icon = { Icon(Icons.Default.Settings, null, tint = Muted) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(R.string.sign_out), color = Surface) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onLogout() },
                    icon = { Icon(Icons.Default.Logout, null, tint = Red) },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(businessName.uppercase(), fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, null)
                        }
                    },
                    actions = {
                        IconButton(onClick = onSync) {
                            Icon(Icons.Outlined.CloudUpload, null, tint = AccentTeal)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Surface
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(Background)
                    .verticalScroll(scrollState)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                        .background(PrimaryBlue)
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    Column {
                        Text(stringResource(R.string.business_overview), color = Surface.copy(alpha = 0.9f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            QuickActionCard(Icons.Default.Inventory, stringResource(R.string.inventory), AccentTeal, Modifier.weight(1f), onOpenStock)
                            QuickActionCard(Icons.Default.ReceiptLong, stringResource(R.string.invoices), Color(0xFF673AB7), Modifier.weight(1f), onOpenInvoices)
                            QuickActionCard(Icons.Default.AccountBalanceWallet, stringResource(R.string.expense), Color(0xFFE91E63), Modifier.weight(1f), onOpenExpenses)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.business_tools),
                    modifier = Modifier.padding(horizontal = 24.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Muted,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(12.dp))

                ToolGrid(onOpenParties, onOpenCashBook, onOpenStock, onOpenBills, onOpenStaff, onOpenExpenses)

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun QuickActionCard(icon: ImageVector, label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier
            .height(90.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Surface.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, color = Surface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ToolGrid(
    onParties: () -> Unit,
    onCash: () -> Unit,
    onStock: () -> Unit,
    onBills: () -> Unit,
    onStaff: () -> Unit,
    onExpense: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tools = listOf(
        Triple(context.getString(R.string.parties), Icons.Default.Groups, onParties),
        Triple(context.getString(R.string.cash), Icons.Default.Payments, onCash),
        Triple(context.getString(R.string.stock), Icons.Default.Store, onStock),
        Triple(context.getString(R.string.bills), Icons.Default.Receipt, onBills),
        Triple(context.getString(R.string.staff), Icons.Default.Person, onStaff),
        Triple(context.getString(R.string.expense), Icons.Default.Analytics, onExpense)
    )

    Column(Modifier.padding(horizontal = 16.dp)) {
        for (i in tools.indices step 3) {
            Row(Modifier.fillMaxWidth()) {
                for (j in 0 until 3) {
                    val index = i + j
                    if (index < tools.size) {
                        ToolItem(tools[index], Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun ToolItem(tool: Triple<String, ImageVector, () -> Unit>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .padding(8.dp)
            .clickable { tool.third() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier.padding(vertical = 20.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(tool.second, null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(tool.first, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
        }
    }
}
