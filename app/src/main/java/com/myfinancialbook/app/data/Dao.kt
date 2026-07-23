package com.myfinancialbook.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyDao {
    @Query("SELECT * FROM parties ORDER BY name ASC")
    fun getAll(): Flow<List<Party>>

    @Query("SELECT * FROM parties WHERE id = :id")
    suspend fun getById(id: Long): Party?

    @Insert
    suspend fun insert(party: Party): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(parties: List<Party>)

    @Update
    suspend fun update(party: Party)

    @Delete
    suspend fun delete(party: Party)

    @Query("DELETE FROM parties")
    suspend fun clearAll()
}

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries WHERE partyId = :partyId ORDER BY timestamp ASC")
    fun getForParty(partyId: String): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM entries")
    fun getAll(): Flow<List<LedgerEntry>>

    @Insert
    suspend fun insert(entry: LedgerEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<LedgerEntry>)

    @Update
    suspend fun update(entry: LedgerEntry)

    @Delete
    suspend fun delete(entry: LedgerEntry)

    @Query("DELETE FROM entries")
    suspend fun clearAll()

    @Query("DELETE FROM entries WHERE partyId = :partyId")
    suspend fun deleteForParty(partyId: String)
}

@Dao
interface CashDao {
    @Query("SELECT * FROM cash_transactions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<CashTransaction>>

    @Insert
    suspend fun insert(tx: CashTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(txes: List<CashTransaction>)

    @Delete
    suspend fun delete(tx: CashTransaction)

    @Query("DELETE FROM cash_transactions")
    suspend fun clearAll()
}

@Dao
interface StockDao {
    @Query("SELECT * FROM stock_items ORDER BY name ASC")
    fun getAll(): Flow<List<StockItem>>

    @Insert
    suspend fun insert(item: StockItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<StockItem>)

    @Update
    suspend fun update(item: StockItem)

    @Delete
    suspend fun delete(item: StockItem)

    @Query("DELETE FROM stock_items")
    suspend fun clearAll()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAll(): Flow<List<Expense>>

    @Insert
    suspend fun insert(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<Expense>)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("DELETE FROM expenses")
    suspend fun clearAll()
}

@Dao
interface InvoiceDao {
    @Query("SELECT * FROM invoices ORDER BY date DESC")
    fun getAll(): Flow<List<Invoice>>

    @Insert
    suspend fun insert(invoice: Invoice): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invoices: List<Invoice>)

    @Update
    suspend fun update(invoice: Invoice)

    @Delete
    suspend fun delete(invoice: Invoice)

    @Query("DELETE FROM invoices")
    suspend fun clearAll()
}

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY date DESC")
    fun getAll(): Flow<List<Bill>>

    @Insert
    suspend fun insert(bill: Bill): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(bills: List<Bill>)

    @Delete
    suspend fun delete(bill: Bill)

    @Query("DELETE FROM bills")
    suspend fun clearAll()
}

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff ORDER BY name ASC")
    fun getAll(): Flow<List<Staff>>

    @Insert
    suspend fun insert(staff: Staff): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(staffs: List<Staff>)

    @Delete
    suspend fun delete(staff: Staff)

    @Query("DELETE FROM staff")
    suspend fun clearAll()
}
