package com.myfinancialbook.app.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.myfinancialbook.app.sync.FirestoreSync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File

class LedgerRepository(private val db: AppDatabase, private val firestore: FirestoreSync? = null) {
    val parties: Flow<List<Party>> = db.partyDao().getAll()
    val allEntries: Flow<List<LedgerEntry>> = db.entryDao().getAll()
    val cashTransactions: Flow<List<CashTransaction>> = db.cashDao().getAll()
    val stockItems: Flow<List<StockItem>> = db.stockDao().getAll()
    val expenses: Flow<List<Expense>> = db.expenseDao().getAll()
    val invoices: Flow<List<Invoice>> = db.invoiceDao().getAll()
    val staff: Flow<List<Staff>> = db.staffDao().getAll()
    val bills: Flow<List<Bill>> = db.billDao().getAll()

    suspend fun createBackupFile(backupFile: File, context: Context): Boolean {
        try {
            val backupDb = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                backupFile.absolutePath
            ).setJournalMode(RoomDatabase.JournalMode.TRUNCATE).build()

            val parties = db.partyDao().getAll().first()
            val entries = db.entryDao().getAll().first()
            val cash = db.cashDao().getAll().first()
            val stock = db.stockDao().getAll().first()
            val expenses = db.expenseDao().getAll().first()
            val invoices = db.invoiceDao().getAll().first()
            val staff = db.staffDao().getAll().first()
            val bills = db.billDao().getAll().first()

            backupDb.partyDao().insertAll(parties)
            backupDb.entryDao().insertAll(entries)
            backupDb.cashDao().insertAll(cash)
            backupDb.stockDao().insertAll(stock)
            backupDb.expenseDao().insertAll(expenses)
            backupDb.invoiceDao().insertAll(invoices)
            backupDb.staffDao().insertAll(staff)
            backupDb.billDao().insertAll(bills)

            backupDb.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            backupDb.close()
            return true
        } catch (e: Exception) {
            android.util.Log.e("Backup", "Failed to create backup file", e)
            return false
        }
    }

    @Transaction
    suspend fun addPartyWithOpening(
        name: String, phone: String, type: String,
        openingType: String?, openingAmount: Double, note: String,
        attachmentPath: String?, voiceNotePath: String?
    ): String {
        val firestoreId = firestore?.createParty(Party(name = name, phone = phone, type = type)) ?: ""
        val party = Party(name = name, phone = phone, type = type, firestoreId = firestoreId)
        val roomId = db.partyDao().insert(party)
        if (openingType != null && openingAmount > 0) {
            val entry = LedgerEntry(partyId = firestoreId, type = openingType, amount = openingAmount,
                note = note, timestamp = System.currentTimeMillis(),
                attachmentPath = attachmentPath, voiceNotePath = voiceNotePath)
            val entryFid = firestore?.createEntry(entry) ?: ""
            db.entryDao().insert(entry.copy(firestoreId = entryFid))
        }
        return firestoreId
    }

    suspend fun updateParty(party: Party) {
        db.partyDao().update(party)
        firestore?.updateParty(party)
    }

    suspend fun deleteParty(party: Party) {
        db.entryDao().deleteForParty(party.firestoreId)
        db.partyDao().delete(party)
        firestore?.deleteParty(party.firestoreId)
    }

    suspend fun getParty(id: Long) = db.partyDao().getById(id)
    fun entriesForParty(partyId: String): Flow<List<LedgerEntry>> = db.entryDao().getForParty(partyId)

    fun checkpoint() {
        db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
    }

    suspend fun restoreFromBackup(backupFile: File, context: Context, statusCallback: ((String) -> Unit)? = null): Boolean {
        try {
            statusCallback?.invoke("Opening backup database...")
            val backupDb = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                backupFile.absolutePath
            ).setJournalMode(RoomDatabase.JournalMode.TRUNCATE).build()

            statusCallback?.invoke("Reading backup data...")
            val backupParties = backupDb.partyDao().getAll().first()
            val backupEntries = backupDb.entryDao().getAll().first()
            val backupCash = backupDb.cashDao().getAll().first()
            val backupStock = backupDb.stockDao().getAll().first()
            val backupExpenses = backupDb.expenseDao().getAll().first()
            val backupInvoices = backupDb.invoiceDao().getAll().first()
            val backupStaff = backupDb.staffDao().getAll().first()
            val backupBills = backupDb.billDao().getAll().first()
            backupDb.close()

            if (backupParties.isEmpty() && backupEntries.isEmpty() && backupCash.isEmpty() &&
                backupStock.isEmpty() && backupExpenses.isEmpty() && backupInvoices.isEmpty() &&
                backupStaff.isEmpty() && backupBills.isEmpty()) {
                statusCallback?.invoke("Backup file is empty!")
                return false
            }

            statusCallback?.invoke("Clearing current data...")
            db.partyDao().clearAll()
            db.entryDao().clearAll()
            db.cashDao().clearAll()
            db.stockDao().clearAll()
            db.expenseDao().clearAll()
            db.invoiceDao().clearAll()
            db.staffDao().clearAll()
            db.billDao().clearAll()

            statusCallback?.invoke("Inserting backup data...")
            db.partyDao().insertAll(backupParties)
            db.entryDao().insertAll(backupEntries)
            db.cashDao().insertAll(backupCash)
            db.stockDao().insertAll(backupStock)
            db.expenseDao().insertAll(backupExpenses)
            db.invoiceDao().insertAll(backupInvoices)
            db.staffDao().insertAll(backupStaff)
            db.billDao().insertAll(backupBills)

            backupFile.delete()
            File(backupFile.absolutePath + "-wal").delete()
            File(backupFile.absolutePath + "-shm").delete()
            statusCallback?.invoke("Done")
            return true
        } catch (e: Exception) {
            android.util.Log.e("Restore", "Restore failed", e)
            statusCallback?.invoke("Error: ${e.message}")
            return false
        }
    }

    suspend fun addEntry(
        partyId: String, type: String, amount: Double, note: String,
        timestamp: Long, attachmentPath: String? = null, voiceNotePath: String? = null
    ) {
        val entry = LedgerEntry(partyId = partyId, type = type, amount = amount, note = note,
            timestamp = timestamp, attachmentPath = attachmentPath, voiceNotePath = voiceNotePath)
        val fid = firestore?.createEntry(entry) ?: ""
        db.entryDao().insert(entry.copy(firestoreId = fid))
    }

    suspend fun deleteEntry(entry: LedgerEntry) {
        db.entryDao().delete(entry)
        firestore?.deleteEntry(entry.firestoreId)
    }

    suspend fun updateEntry(entry: LedgerEntry) {
        db.entryDao().update(entry)
        firestore?.writeEntry(entry)
    }

    suspend fun addCashTransaction(amount: Double, type: String, note: String) {
        val tx = CashTransaction(amount = amount, type = type, note = note)
        val fid = firestore?.createCashTransaction(tx) ?: ""
        db.cashDao().insert(tx.copy(firestoreId = fid))
    }

    suspend fun deleteCashTransaction(tx: CashTransaction) {
        db.cashDao().delete(tx)
        firestore?.deleteCashTransaction(tx.firestoreId)
    }

    suspend fun addStockItem(name: String, quantity: Double, purchasePrice: Double, salePrice: Double) {
        val item = StockItem(name = name, quantity = quantity, purchasePrice = purchasePrice, salePrice = salePrice)
        val fid = firestore?.createStockItem(item) ?: ""
        db.stockDao().insert(item.copy(firestoreId = fid))
    }

    suspend fun deleteStockItem(item: StockItem) {
        db.stockDao().delete(item)
        firestore?.deleteStockItem(item.firestoreId)
    }

    suspend fun addExpense(amount: Double, category: String, note: String) {
        val expense = Expense(amount = amount, category = category, note = note)
        val fid = firestore?.createExpense(expense) ?: ""
        db.expenseDao().insert(expense.copy(firestoreId = fid))
    }

    suspend fun deleteExpense(expense: Expense) {
        db.expenseDao().delete(expense)
        firestore?.deleteExpense(expense.firestoreId)
    }

    suspend fun addInvoice(partyId: String, amount: Double, invoiceNum: String, items: String) {
        val invoice = Invoice(partyId = partyId, amount = amount, invoiceNumber = invoiceNum, itemsJson = items)
        val fid = firestore?.createInvoice(invoice) ?: ""
        db.invoiceDao().insert(invoice.copy(firestoreId = fid))
    }

    suspend fun updateInvoice(invoice: Invoice) {
        db.invoiceDao().update(invoice)
        firestore?.writeInvoice(invoice)
    }

    suspend fun deleteInvoice(invoice: Invoice) {
        db.invoiceDao().delete(invoice)
        firestore?.deleteInvoice(invoice.firestoreId)
    }

    suspend fun addBill(billNum: String, provider: String, amount: Double) {
        val bill = Bill(billNumber = billNum, provider = provider, amount = amount)
        val fid = firestore?.createBill(bill) ?: ""
        db.billDao().insert(bill.copy(firestoreId = fid))
    }

    suspend fun deleteBill(bill: Bill) {
        db.billDao().delete(bill)
        firestore?.deleteBill(bill.firestoreId)
    }

    suspend fun addStaff(name: String, phone: String, salary: Double) {
        val staff = Staff(name = name, phone = phone, salary = salary)
        val fid = firestore?.createStaff(staff) ?: ""
        db.staffDao().insert(staff.copy(firestoreId = fid))
    }

    suspend fun deleteStaff(staff: Staff) {
        db.staffDao().delete(staff)
        firestore?.deleteStaff(staff.firestoreId)
    }

    suspend fun syncFromCloud(): Boolean {
        val data = firestore?.pullAllFromCloud() ?: return false
        db.partyDao().clearAll()
        db.entryDao().clearAll()
        db.cashDao().clearAll()
        db.stockDao().clearAll()
        db.expenseDao().clearAll()
        db.invoiceDao().clearAll()
        db.staffDao().clearAll()
        db.billDao().clearAll()
        if (data.parties.isNotEmpty()) db.partyDao().insertAll(data.parties)
        if (data.entries.isNotEmpty()) db.entryDao().insertAll(data.entries)
        if (data.cash.isNotEmpty()) db.cashDao().insertAll(data.cash)
        if (data.stock.isNotEmpty()) db.stockDao().insertAll(data.stock)
        if (data.expenses.isNotEmpty()) db.expenseDao().insertAll(data.expenses)
        if (data.invoices.isNotEmpty()) db.invoiceDao().insertAll(data.invoices)
        if (data.staff.isNotEmpty()) db.staffDao().insertAll(data.staff)
        if (data.bills.isNotEmpty()) db.billDao().insertAll(data.bills)
        return true
    }
}

fun balanceOf(entries: List<LedgerEntry>): Double =
    entries.sumOf { if (it.type == "GET") it.amount else -it.amount }
