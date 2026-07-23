package com.myfinancialbook.app.sync

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myfinancialbook.app.data.*
import kotlinx.coroutines.tasks.await

class FirestoreSync(context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private fun userCol(collection: String) =
        auth.currentUser?.uid?.let { uid -> db.collection("users").document(uid).collection(collection) }

    private fun userDoc(collection: String, docId: String) =
        auth.currentUser?.uid?.let { uid -> db.collection("users").document(uid).collection(collection).document(docId) }

    // Create: use Firestore auto-ID, return it
    private suspend fun createDoc(collection: String, data: Map<String, Any?>): String {
        val ref = userCol(collection)?.add(data)?.await()
        return ref?.id ?: throw Exception("Failed to create $collection document")
    }

    // Update: set data on existing doc
    private suspend fun setDoc(collection: String, docId: String, data: Map<String, Any?>) {
        userDoc(collection, docId)?.set(data)?.await()
    }

    // Delete
    private suspend fun deleteDoc(collection: String, docId: String) {
        userDoc(collection, docId)?.delete()?.await()
    }

    // ===== WRITE OPERATIONS =====

    suspend fun writeParty(party: Party) {
        try {
            val data = mapOf("name" to party.name, "phone" to party.phone, "type" to party.type, "createdAt" to party.createdAt)
            if (party.firestoreId.isNotBlank()) {
                setDoc("parties", party.firestoreId, data)
            } else {
                val fid = createDoc("parties", data)
                party.copy(firestoreId = fid) // caller must save this
            }
        } catch (e: Exception) { Log.e("FirestoreSync", "writeParty failed", e) }
    }

    suspend fun createParty(party: Party): String {
        val data = mapOf("name" to party.name, "phone" to party.phone, "type" to party.type, "createdAt" to party.createdAt)
        return createDoc("parties", data)
    }

    suspend fun updateParty(party: Party) {
        try { setDoc("parties", party.firestoreId, mapOf("name" to party.name, "phone" to party.phone, "type" to party.type)) }
        catch (e: Exception) { Log.e("FirestoreSync", "updateParty failed", e) }
    }

    suspend fun deleteParty(partyId: String) {
        try { deleteDoc("parties", partyId) } catch (e: Exception) { Log.e("FirestoreSync", "deleteParty failed", e) }
    }

    suspend fun createEntry(entry: LedgerEntry): String {
        val data = mapOf("partyId" to entry.partyId, "type" to entry.type, "amount" to entry.amount,
            "note" to entry.note, "timestamp" to entry.timestamp)
        return createDoc("entries", data)
    }

    suspend fun writeEntry(entry: LedgerEntry) {
        try { setDoc("entries", entry.firestoreId, mapOf("partyId" to entry.partyId, "type" to entry.type,
            "amount" to entry.amount, "note" to entry.note, "timestamp" to entry.timestamp)) }
        catch (e: Exception) { Log.e("FirestoreSync", "writeEntry failed", e) }
    }

    suspend fun deleteEntry(entryId: String) {
        try { deleteDoc("entries", entryId) } catch (e: Exception) { Log.e("FirestoreSync", "deleteEntry failed", e) }
    }

    suspend fun createCashTransaction(tx: CashTransaction): String {
        val data = mapOf("amount" to tx.amount, "type" to tx.type, "note" to tx.note, "timestamp" to tx.timestamp)
        return createDoc("cash", data)
    }

    suspend fun writeCashTransaction(tx: CashTransaction) {
        try { setDoc("cash", tx.firestoreId, mapOf("amount" to tx.amount, "type" to tx.type, "note" to tx.note, "timestamp" to tx.timestamp)) }
        catch (e: Exception) { Log.e("FirestoreSync", "writeCashTransaction failed", e) }
    }

    suspend fun deleteCashTransaction(txId: String) {
        try { deleteDoc("cash", txId) } catch (e: Exception) { Log.e("FirestoreSync", "deleteCashTransaction failed", e) }
    }

    suspend fun createStockItem(item: StockItem): String {
        val data = mapOf("name" to item.name, "quantity" to item.quantity, "purchasePrice" to item.purchasePrice, "salePrice" to item.salePrice, "unit" to item.unit)
        return createDoc("stock", data)
    }

    suspend fun writeStockItem(item: StockItem) {
        try { setDoc("stock", item.firestoreId, mapOf("name" to item.name, "quantity" to item.quantity,
            "purchasePrice" to item.purchasePrice, "salePrice" to item.salePrice, "unit" to item.unit)) }
        catch (e: Exception) { Log.e("FirestoreSync", "writeStockItem failed", e) }
    }

    suspend fun deleteStockItem(itemId: String) {
        try { deleteDoc("stock", itemId) } catch (e: Exception) { Log.e("FirestoreSync", "deleteStockItem failed", e) }
    }

    suspend fun createExpense(expense: Expense): String {
        val data = mapOf("amount" to expense.amount, "category" to expense.category, "note" to expense.note, "timestamp" to expense.timestamp)
        return createDoc("expenses", data)
    }

    suspend fun writeExpense(expense: Expense) {
        try { setDoc("expenses", expense.firestoreId, mapOf("amount" to expense.amount, "category" to expense.category, "note" to expense.note, "timestamp" to expense.timestamp)) }
        catch (e: Exception) { Log.e("FirestoreSync", "writeExpense failed", e) }
    }

    suspend fun deleteExpense(expenseId: String) {
        try { deleteDoc("expenses", expenseId) } catch (e: Exception) { Log.e("FirestoreSync", "deleteExpense failed", e) }
    }

    suspend fun createInvoice(invoice: Invoice): String {
        val data = mapOf("invoiceNumber" to invoice.invoiceNumber, "partyId" to invoice.partyId,
            "amount" to invoice.amount, "itemsJson" to invoice.itemsJson, "date" to invoice.date, "status" to invoice.status)
        return createDoc("invoices", data)
    }

    suspend fun writeInvoice(invoice: Invoice) {
        try { setDoc("invoices", invoice.firestoreId, mapOf("invoiceNumber" to invoice.invoiceNumber, "partyId" to invoice.partyId,
            "amount" to invoice.amount, "itemsJson" to invoice.itemsJson, "date" to invoice.date, "status" to invoice.status)) }
        catch (e: Exception) { Log.e("FirestoreSync", "writeInvoice failed", e) }
    }

    suspend fun deleteInvoice(invoiceId: String) {
        try { deleteDoc("invoices", invoiceId) } catch (e: Exception) { Log.e("FirestoreSync", "deleteInvoice failed", e) }
    }

    suspend fun createStaff(staff: Staff): String {
        val data = mapOf("name" to staff.name, "phone" to staff.phone, "salary" to staff.salary, "role" to staff.role)
        return createDoc("staff", data)
    }

    suspend fun writeStaff(staff: Staff) {
        try { setDoc("staff", staff.firestoreId, mapOf("name" to staff.name, "phone" to staff.phone, "salary" to staff.salary, "role" to staff.role)) }
        catch (e: Exception) { Log.e("FirestoreSync", "writeStaff failed", e) }
    }

    suspend fun deleteStaff(staffId: String) {
        try { deleteDoc("staff", staffId) } catch (e: Exception) { Log.e("FirestoreSync", "deleteStaff failed", e) }
    }

    suspend fun createBill(bill: Bill): String {
        val data = mapOf("billNumber" to bill.billNumber, "provider" to bill.provider, "amount" to bill.amount, "date" to bill.date, "status" to bill.status)
        return createDoc("bills", data)
    }

    suspend fun writeBill(bill: Bill) {
        try { setDoc("bills", bill.firestoreId, mapOf("billNumber" to bill.billNumber, "provider" to bill.provider, "amount" to bill.amount, "date" to bill.date, "status" to bill.status)) }
        catch (e: Exception) { Log.e("FirestoreSync", "writeBill failed", e) }
    }

    suspend fun deleteBill(billId: String) {
        try { deleteDoc("bills", billId) } catch (e: Exception) { Log.e("FirestoreSync", "deleteBill failed", e) }
    }

    // ===== READ ALL (pull from cloud) =====

    suspend fun pullAllFromCloud(): CloudData? {
        return try {
            val uid = auth.currentUser?.uid ?: return null
            val batch = db.collection("users").document(uid)

            val parties = batch.collection("parties").get().await().documents.map { doc ->
                Party(id = 0, firestoreId = doc.id,
                    name = doc.getString("name") ?: "",
                    phone = doc.getString("phone") ?: "",
                    type = doc.getString("type") ?: "CUSTOMER",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis())
            }
            val entries = batch.collection("entries").get().await().documents.map { doc ->
                LedgerEntry(id = 0, firestoreId = doc.id,
                    partyId = doc.getString("partyId") ?: "",
                    type = doc.getString("type") ?: "",
                    amount = doc.getDouble("amount") ?: 0.0,
                    note = doc.getString("note") ?: "",
                    attachmentPath = doc.getString("attachmentPath"),
                    voiceNotePath = doc.getString("voiceNotePath"),
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis())
            }
            val cash = batch.collection("cash").get().await().documents.map { doc ->
                CashTransaction(id = 0, firestoreId = doc.id,
                    amount = doc.getDouble("amount") ?: 0.0,
                    type = doc.getString("type") ?: "",
                    note = doc.getString("note") ?: "",
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis())
            }
            val stock = batch.collection("stock").get().await().documents.map { doc ->
                StockItem(id = 0, firestoreId = doc.id,
                    name = doc.getString("name") ?: "",
                    quantity = doc.getDouble("quantity") ?: 0.0,
                    purchasePrice = doc.getDouble("purchasePrice") ?: 0.0,
                    salePrice = doc.getDouble("salePrice") ?: 0.0,
                    unit = doc.getString("unit") ?: "pcs")
            }
            val expenses = batch.collection("expenses").get().await().documents.map { doc ->
                Expense(id = 0, firestoreId = doc.id,
                    amount = doc.getDouble("amount") ?: 0.0,
                    category = doc.getString("category") ?: "",
                    note = doc.getString("note") ?: "",
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis())
            }
            val invoices = batch.collection("invoices").get().await().documents.map { doc ->
                Invoice(id = 0, firestoreId = doc.id,
                    invoiceNumber = doc.getString("invoiceNumber") ?: "",
                    partyId = doc.getString("partyId") ?: "",
                    amount = doc.getDouble("amount") ?: 0.0,
                    itemsJson = doc.getString("itemsJson") ?: "",
                    date = doc.getLong("date") ?: System.currentTimeMillis(),
                    status = doc.getString("status") ?: "PENDING")
            }
            val staff = batch.collection("staff").get().await().documents.map { doc ->
                Staff(id = 0, firestoreId = doc.id,
                    name = doc.getString("name") ?: "",
                    phone = doc.getString("phone") ?: "",
                    salary = doc.getDouble("salary") ?: 0.0,
                    role = doc.getString("role") ?: "Staff")
            }
            val bills = batch.collection("bills").get().await().documents.map { doc ->
                Bill(id = 0, firestoreId = doc.id,
                    billNumber = doc.getString("billNumber") ?: "",
                    provider = doc.getString("provider") ?: "",
                    amount = doc.getDouble("amount") ?: 0.0,
                    date = doc.getLong("date") ?: System.currentTimeMillis(),
                    status = doc.getString("status") ?: "UNPAID")
            }

            CloudData(parties, entries, cash, stock, expenses, invoices, staff, bills)
        } catch (e: Exception) {
            Log.e("FirestoreSync", "pullAllFromCloud failed", e)
            null
        }
    }

    data class CloudData(
        val parties: List<Party>,
        val entries: List<LedgerEntry>,
        val cash: List<CashTransaction>,
        val stock: List<StockItem>,
        val expenses: List<Expense>,
        val invoices: List<Invoice>,
        val staff: List<Staff>,
        val bills: List<Bill>
    )
}
