package com.myfinancialbook.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parties")
data class Party(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val type: String = "CUSTOMER",
    val createdAt: Long = System.currentTimeMillis(),
    val firestoreId: String = ""
)

@Entity(tableName = "entries")
data class LedgerEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partyId: String = "",
    val type: String,
    val amount: Double,
    val note: String = "",
    val attachmentPath: String? = null,
    val voiceNotePath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val firestoreId: String = ""
)

@Entity(tableName = "cash_transactions")
data class CashTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val firestoreId: String = ""
)

@Entity(tableName = "stock_items")
data class StockItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Double,
    val purchasePrice: Double,
    val salePrice: Double,
    val unit: String = "pcs",
    val firestoreId: String = ""
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val category: String,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val firestoreId: String = ""
)

@Entity(tableName = "invoices")
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val partyId: String = "",
    val amount: Double,
    val itemsJson: String = "",
    val date: Long = System.currentTimeMillis(),
    val status: String = "PENDING",
    val firestoreId: String = ""
)

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billNumber: String,
    val provider: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val status: String = "UNPAID",
    val firestoreId: String = ""
)

@Entity(tableName = "staff")
data class Staff(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val salary: Double = 0.0,
    val role: String = "Staff",
    val firestoreId: String = ""
)
