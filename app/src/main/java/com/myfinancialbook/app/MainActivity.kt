package com.myfinancialbook.app

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.myfinancialbook.app.ui.*
import com.myfinancialbook.app.ui.theme.MyFinancialBookTheme
import com.myfinancialbook.app.util.AuthManager
import com.myfinancialbook.app.util.LocaleHelper
import com.myfinancialbook.app.util.SecurityManager
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val vm: LedgerViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val lang = LocaleHelper.getLanguage(newBase)
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyFinancialBookTheme {
                val onboardingComplete by vm.onboardingComplete.collectAsState()

                val googleAuthLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    try {
                        val account = task.getResult(ApiException::class.java)
                        val idToken = account?.idToken
                        if (idToken != null) {
                            kotlinx.coroutines.MainScope().launch {
                                AuthManager.signInWithGoogle(idToken)
                                vm.notifyGoogleSignInComplete()
                            }
                        } else {
                            Toast.makeText(this@MainActivity, "Google sign-in failed: no ID token", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: ApiException) {
                        Toast.makeText(this@MainActivity, "Google sign-in failed: ${e.statusCode}", Toast.LENGTH_LONG).show()
                    }
                }

                if (!onboardingComplete) {
                    OnboardingScreen(
                        vm = vm,
                        onGoogleSignInRequest = {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken(getString(R.string.default_web_client_id))
                                .requestEmail()
                                .build()
                            val client = GoogleSignIn.getClient(this@MainActivity, gso)
                            googleAuthLauncher.launch(client.signInIntent)
                        }
                    )
                } else {
                    MainContent()
                }
            }
        }
    }

    @Composable
    fun MainContent() {
        val navController = rememberNavController()
        var showProfile by remember { mutableStateOf(false) }
        var showReports by remember { mutableStateOf(false) }
        var isUnlocked by remember { mutableStateOf(!SecurityManager.hasBackupPin(this@MainActivity)) }
        var pinInput by remember { mutableStateOf("") }
        val businessName by vm.businessName.collectAsState()
        val syncStatus by vm.syncStatus.collectAsState()

        val googleSignInLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                task.getResult(ApiException::class.java)
                Toast.makeText(this@MainActivity, "Signed in successfully", Toast.LENGTH_SHORT).show()
                vm.syncToDrive()
            } catch (e: ApiException) {
                val msg = when (e.statusCode) {
                    10 -> "Developer Error: Check SHA-1 in Google Console"
                    7 -> "Network Error: Check Internet"
                    else -> "Sign in failed: ${e.statusCode}"
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }

        fun triggerSync() {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
                .build()
            val client = GoogleSignIn.getClient(this@MainActivity, gso)
            googleSignInLauncher.launch(client.signInIntent)
        }

        LaunchedEffect(syncStatus) {
            syncStatus?.let {
                Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
            }
        }

        if (!isUnlocked) {
            LockScreen(onUnlock = { isUnlocked = true }, pinInput = pinInput, onPinChange = { pinInput = it })
        } else {
            NavigationHost(navController, showProfile = { showProfile = true }, showReports = { showReports = true }, triggerSync = { triggerSync() })

            if (showProfile) {
                ProfileDialog(
                    currentName = businessName,
                    onDismiss = { showProfile = false },
                    onSave = { name -> vm.setBusinessName(name); showProfile = false },
                    onLanguageChange = { lang ->
                        LocaleHelper.persistLanguage(this@MainActivity, lang)
                        recreate()
                    },
                    onToggleBiometric = { enabled ->
                        SecurityManager.setBiometricEnabled(this@MainActivity, enabled)
                    },
                    onPinChange = { pin ->
                        SecurityManager.setBackupPin(this@MainActivity, pin)
                    },
                    onRestore = {
                        vm.restoreFromDrive()
                    },
                    onSyncFromCloud = {
                        vm.syncFromCloud()
                    },
                    onLogout = {
                        vm.signOut()
                        recreate()
                    }
                )
            }
            if (showReports) {
                ReportsDialog(vm = vm, onDismiss = { showReports = false })
            }
        }
    }

    @Composable
    fun LockScreen(onUnlock: () -> Unit, pinInput: String, onPinChange: (String) -> Unit) {
        if (SecurityManager.isBiometricEnabled(this@MainActivity)) {
            LaunchedEffect(Unit) {
                SecurityManager.showBiometricPrompt(
                    activity = this@MainActivity,
                    onSuccess = onUnlock,
                    onError = { }
                )
            }
        }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(Icons.Default.Lock, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
                Text("Enter PIN to Access", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            onPinChange(it)
                            if (it.length == 4 && SecurityManager.validatePin(this@MainActivity, it)) {
                                onUnlock()
                            }
                        }
                    },
                    label = { Text("4-Digit PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                if (SecurityManager.isBiometricAvailable(this@MainActivity) && SecurityManager.isBiometricEnabled(this@MainActivity)) {
                    TextButton(
                        onClick = {
                            SecurityManager.showBiometricPrompt(
                                activity = this@MainActivity,
                                onSuccess = onUnlock,
                                onError = { Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show() }
                            )
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Use Fingerprint")
                    }
                }
            }
        }
    }

    @Composable
    fun NavigationHost(navController: androidx.navigation.NavHostController, showProfile: () -> Unit, showReports: () -> Unit, triggerSync: () -> Unit) {
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    vm = vm,
                    onOpenParty = { id -> navController.navigate("party/$id") },
                    onOpenProfile = showProfile,
                    onOpenReports = showReports,
                    onOpenCashBook = { navController.navigate("cashbook") },
                    onOpenParties = { navController.navigate("parties") },
                    onOpenStock = { navController.navigate("stock") },
                    onOpenExpenses = { navController.navigate("expenses") },
                    onOpenBills = { navController.navigate("bills") },
                    onOpenStaff = { navController.navigate("staff") },
                    onOpenInvoices = { navController.navigate("invoices") },
                    onSync = triggerSync,
                    onRestore = { vm.restoreFromDrive() },
                    onLogout = {
                        vm.signOut()
                        recreate()
                    }
                )
            }
            composable("parties") {
                PartiesScreen(
                    vm = vm,
                    onOpenParty = { id -> navController.navigate("party/$id") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("cashbook") {
                CashBookScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable("stock") {
                StockScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable("expenses") {
                ExpenseScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable("bills") {
                BillsScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable("staff") {
                StaffScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable("invoices") {
                InvoicesScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable(
                "party/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                PartyDetailScreen(
                    vm = vm,
                    partyId = id,
                    onBack = { navController.popBackStack() },
                    onAddEntry = { type -> navController.navigate("add_entry/$id/$type") },
                    onEditEntry = { entryId -> navController.navigate("edit_entry/$id/$entryId") }
                )
            }
            composable(
                "add_entry/{id}/{type}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("type") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                val type = backStackEntry.arguments?.getString("type") ?: "GET"
                AddEntryScreen(vm = vm, partyId = id, type = type, onBack = { navController.popBackStack() })
            }
            composable(
                "edit_entry/{partyId}/{entryId}",
                arguments = listOf(
                    navArgument("partyId") { type = NavType.StringType },
                    navArgument("entryId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val partyId = backStackEntry.arguments?.getString("partyId") ?: ""
                val entryId = backStackEntry.arguments?.getLong("entryId") ?: 0L
                val existingEntry = vm.getEntrySync(entryId)
                AddEntryScreen(vm = vm, partyId = partyId, type = existingEntry?.type ?: "GET", entryId = entryId, onBack = { navController.popBackStack() })
            }
        }
    }
}
