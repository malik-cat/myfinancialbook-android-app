package com.myfinancialbook.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myfinancialbook.app.data.*
import com.myfinancialbook.app.sync.FirestoreSync
import com.myfinancialbook.app.sync.GoogleDriveManager
import com.myfinancialbook.app.util.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class LedgerViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = LedgerRepository(AppDatabase.getInstance(app), firestore = FirestoreSync(app))
    private val syncManager = GoogleDriveManager(app)
    private val prefs = app.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _onboardingComplete = MutableStateFlow(prefs.getBoolean("onboarding_complete", false))
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete

    private val _isVerified = MutableStateFlow(prefs.getBoolean("is_verified", false))
    val isVerified: StateFlow<Boolean> = _isVerified

    private val _googleSignInEvent = MutableStateFlow(0)
    val googleSignInEvent: StateFlow<Int> = _googleSignInEvent

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_complete", true).apply()
        _onboardingComplete.value = true
    }

    fun setVerified(verified: Boolean) {
        prefs.edit().putBoolean("is_verified", verified).apply()
        _isVerified.value = verified
    }

    fun notifyGoogleSignInComplete() {
        _googleSignInEvent.value = _googleSignInEvent.value + 1
    }

    private val _syncStatus = MutableStateFlow<String?>(null)
    val syncStatus: StateFlow<String?> = _syncStatus

    fun syncToDrive() {
        viewModelScope.launch {
            _syncStatus.value = "Checking for backup..."
            try {
                val app = getApplication<Application>()
                var shouldUpload = true
                val currentParties = repo.parties.first()
                if (currentParties.isEmpty()) {
                    _syncStatus.value = "Searching for backup on Drive..."
                    val tempFile = File(app.cacheDir, "sync_restore_temp.db")
                    val downloaded = syncManager.downloadBackupTo(tempFile)
                    if (downloaded) {
                        _syncStatus.value = "Restoring data from backup..."
                        val restored = repo.restoreFromBackup(tempFile, app) { step ->
                            _syncStatus.value = step
                        }
                        if (restored) {
                            _syncStatus.value = "Data Restored from backup!"
                        } else {
                            _syncStatus.value = "Restore failed. Check app logs for details."
                        }
                        return@launch
                    }
                    shouldUpload = false
                }
                if (shouldUpload) {
                    _syncStatus.value = "Creating backup from data..."
                    val backupFile = File(app.cacheDir, "backup_upload_temp.db")
                    val created = repo.createBackupFile(backupFile, app)
                    if (created) {
                        syncManager.uploadBackup(backupFile)
                        backupFile.delete()
                        File(backupFile.absolutePath + "-wal").delete()
                        File(backupFile.absolutePath + "-shm").delete()
                        _syncStatus.value = "Sync successful (Backup saved)"
                    } else {
                        _syncStatus.value = "Backup creation failed"
                    }
                } else {
                    _syncStatus.value = "Sync skipped: no data to upload and no backup on Drive."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val rawMsg = e.message ?: "Unknown error"
                _syncStatus.value = if (rawMsg.contains("403")) {
                    "Sync failed (403): Google Drive API is DISABLED. You MUST enable it in the Google Cloud Console for your project."
                } else {
                    "Sync failed: $rawMsg"
                }
            }
        }
    }

    fun syncFromCloud() {
        viewModelScope.launch {
            _syncStatus.value = "Syncing from cloud..."
            try {
                val success = repo.syncFromCloud()
                _syncStatus.value = if (success) "Cloud sync complete!" else "No user signed in or Firestore not enabled."
            } catch (e: Exception) {
                _syncStatus.value = "Cloud sync failed: ${e.message}"
            }
        }
    }

    fun signOut() {
        AuthManager.signOut()
        prefs.edit().clear().apply()
        _onboardingComplete.value = false
        _isVerified.value = false
    }

    fun restoreFromDrive() {
        viewModelScope.launch {
            _syncStatus.value = "Searching for backup on Drive..."
            try {
                val app = getApplication<Application>()
                val tempFile = File(app.cacheDir, "restore_temp.db")
                val downloaded = syncManager.downloadBackupTo(tempFile)
                if (!downloaded) {
                    _syncStatus.value = "No backup found on Google Drive."
                    return@launch
                }
                _syncStatus.value = "Backup downloaded, restoring data..."
                val restored = repo.restoreFromBackup(tempFile, app) { step ->
                    _syncStatus.value = step
                }
                if (restored) {
                    _syncStatus.value = "SUCCESS: Data Restored!"
                } else {
                    _syncStatus.value = "Restore failed. Check app logs for details."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _syncStatus.value = "Restore crashed: ${e.message}"
            }
        }
    }

    private val _businessName = MutableStateFlow(prefs.getString("business_name", "My Business") ?: "My Business")
    val businessName: StateFlow<String> = _businessName

    val partiesWithBalance: StateFlow<List<PartyUi>> = combine(repo.parties, repo.allEntries) { parties, entries ->
        val byParty = entries.groupBy { it.partyId }
        parties.map { p -> PartyUi(p, balanceOf(byParty[p.firestoreId] ?: emptyList())) }
    }.let { flow ->
        val state = MutableStateFlow<List<PartyUi>>(emptyList())
        viewModelScope.launch { flow.collect { state.value = it } }
        state
    }

    val entriesByPartyId: StateFlow<Map<String, List<LedgerEntry>>> = repo.allEntries.let { flow ->
        val state = MutableStateFlow<Map<String, List<LedgerEntry>>>(emptyMap())
        viewModelScope.launch { flow.collect { entries -> state.value = entries.groupBy { it.partyId } } }
        state
    }

    val cashTransactions: StateFlow<List<CashTransaction>> = repo.cashTransactions.let { flow ->
        val state = MutableStateFlow<List<CashTransaction>>(emptyList())
        viewModelScope.launch { flow.collect { state.value = it } }
        state
    }

    val stockItems: StateFlow<List<StockItem>> = repo.stockItems.let { flow ->
        val state = MutableStateFlow<List<StockItem>>(emptyList())
        viewModelScope.launch { flow.collect { state.value = it } }
        state
    }

    val expenses: StateFlow<List<Expense>> = repo.expenses.let { flow ->
        val state = MutableStateFlow<List<Expense>>(emptyList())
        viewModelScope.launch { flow.collect { state.value = it } }
        state
    }

    val invoices: StateFlow<List<Invoice>> = repo.invoices.let { flow ->
        val state = MutableStateFlow<List<Invoice>>(emptyList())
        viewModelScope.launch { flow.collect { state.value = it } }
        state
    }

    val staff: StateFlow<List<Staff>> = repo.staff.let { flow ->
        val state = MutableStateFlow<List<Staff>>(emptyList())
        viewModelScope.launch { flow.collect { state.value = it } }
        state
    }

    val bills: StateFlow<List<Bill>> = repo.bills.let { flow ->
        val state = MutableStateFlow<List<Bill>>(emptyList())
        viewModelScope.launch { flow.collect { state.value = it } }
        state
    }

    fun setBusinessName(name: String) {
        prefs.edit().putString("business_name", name).apply()
        _businessName.value = name
    }

    fun addParty(
        name: String, phone: String, openingType: String?, openingAmount: Double,
        type: String = "CUSTOMER", note: String = "Opening balance",
        attachmentPath: String? = null, voiceNotePath: String? = null,
        onDone: (String) -> Unit
    ) {
        viewModelScope.launch {
            val firestoreId = repo.addPartyWithOpening(name, phone, type, openingType, openingAmount, note, attachmentPath, voiceNotePath)
            onDone(firestoreId)
        }
    }

    fun updateParty(party: Party) = viewModelScope.launch { repo.updateParty(party) }
    fun deleteParty(party: Party) = viewModelScope.launch { repo.deleteParty(party) }

    fun addEntry(
        partyId: String, type: String, amount: Double, note: String, timestamp: Long,
        attachmentPath: String? = null, voiceNotePath: String? = null
    ) {
        viewModelScope.launch { repo.addEntry(partyId, type, amount, note, timestamp, attachmentPath, voiceNotePath) }
    }

    fun deleteEntry(entry: LedgerEntry) = viewModelScope.launch { repo.deleteEntry(entry) }
    fun updateEntry(entry: LedgerEntry) = viewModelScope.launch { repo.updateEntry(entry) }

    fun addCashTransaction(amount: Double, type: String, note: String) {
        viewModelScope.launch { repo.addCashTransaction(amount, type, note) }
    }
    fun deleteCashTransaction(tx: CashTransaction) = viewModelScope.launch { repo.deleteCashTransaction(tx) }

    fun addStockItem(name: String, quantity: Double, purchasePrice: Double, salePrice: Double) {
        viewModelScope.launch { repo.addStockItem(name, quantity, purchasePrice, salePrice) }
    }
    fun deleteStockItem(item: StockItem) = viewModelScope.launch { repo.deleteStockItem(item) }

    fun addExpense(amount: Double, category: String, note: String) {
        viewModelScope.launch { repo.addExpense(amount, category, note) }
    }
    fun deleteExpense(expense: Expense) = viewModelScope.launch { repo.deleteExpense(expense) }

    fun addInvoice(partyId: String, amount: Double, invoiceNum: String, items: String) {
        viewModelScope.launch { repo.addInvoice(partyId, amount, invoiceNum, items) }
    }
    fun updateInvoice(invoice: Invoice) = viewModelScope.launch { repo.updateInvoice(invoice) }
    fun deleteInvoice(invoice: Invoice) = viewModelScope.launch { repo.deleteInvoice(invoice) }

    fun addBill(billNum: String, provider: String, amount: Double) {
        viewModelScope.launch { repo.addBill(billNum, provider, amount) }
    }
    fun deleteBill(bill: Bill) = viewModelScope.launch { repo.deleteBill(bill) }

    fun addStaff(name: String, phone: String, salary: Double) {
        viewModelScope.launch { repo.addStaff(name, phone, salary) }
    }
    fun deleteStaff(staff: Staff) = viewModelScope.launch { repo.deleteStaff(staff) }

    suspend fun getParty(id: Long): Party? = repo.getParty(id)

    fun getEntrySync(entryId: Long): LedgerEntry? {
        return entriesByPartyId.value.values.flatten().find { it.id == entryId }
    }

    fun findPartyByFirestoreId(firestoreId: String): Party? {
        return partiesWithBalance.value.find { it.party.firestoreId == firestoreId }?.party
    }
}

data class PartyUi(val party: Party, val balance: Double)
