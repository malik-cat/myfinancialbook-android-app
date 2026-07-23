package com.myfinancialbook.app.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.myfinancialbook.app.data.Invoice
import com.myfinancialbook.app.data.LedgerEntry
import com.myfinancialbook.app.data.Party
import com.myfinancialbook.app.data.balanceOf
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfExporter {

    private val dateFmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    private val simpleDateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private fun money(v: Double) = "PKR " + String.format(Locale.getDefault(), "%,.2f", Math.abs(v))

    fun exportInvoice(context: Context, businessName: String, partyName: String, invoice: Invoice): File {
        val pageWidth = 595
        val pageHeight = 842
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        var y = 60f

        val titlePaint = Paint().apply { textSize = 24f; isFakeBoldText = true; color = 0xFF1A237E.toInt() }
        val headerLabelPaint = Paint().apply { textSize = 10f; color = 0xFF888888.toInt(); isFakeBoldText = true }
        val headerValuePaint = Paint().apply { textSize = 12f; isFakeBoldText = true }
        val tableHeaderPaint = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }
        val footerPaint = Paint().apply { textSize = 10f; color = 0xFF666666.toInt(); textAlign = Paint.Align.CENTER }

        // Header
        canvas.drawText(businessName.uppercase(), 40f, y, titlePaint); y += 12f
        canvas.drawText("Tax Invoice", 40f, y, Paint().apply { textSize = 10f; color = 0xFF666666.toInt() }); y += 40f

        // Info Row
        val leftX = 40f
        val rightX = 400f
        
        canvas.drawText("BILL TO", leftX, y, headerLabelPaint)
        canvas.drawText("INVOICE #", rightX, y, headerLabelPaint); y += 16f
        canvas.drawText(partyName, leftX, y, headerValuePaint)
        canvas.drawText(invoice.invoiceNumber, rightX, y, headerValuePaint); y += 24f
        
        canvas.drawText("DATE", rightX, y, headerLabelPaint); y += 16f
        canvas.drawText(simpleDateFmt.format(Date(invoice.date)), rightX, y, headerValuePaint); y += 40f

        // Table Header
        canvas.drawLine(40f, y, 555f, y, Paint().apply { strokeWidth = 1.5f; color = 0xFF000000.toInt() }); y += 18f
        canvas.drawText("DESCRIPTION", 45f, y, tableHeaderPaint)
        canvas.drawText("AMOUNT", 480f, y, tableHeaderPaint); y += 10f
        canvas.drawLine(40f, y, 555f, y, Paint().apply { strokeWidth = 1f; color = 0xFFCCCCCC.toInt() }); y += 22f

        // Items
        // Items format in itemsJson: "Desc1|Amt1;Desc2|Amt2"
        val items = invoice.itemsJson.split(";").filter { it.contains("|") }
        for (item in items) {
            val parts = item.split("|")
            val desc = parts[0]
            val amt = parts[1].toDoubleOrNull() ?: 0.0
            
            canvas.drawText(desc, 45f, y, bodyPaint)
            canvas.drawText(money(amt), 480f, y, bodyPaint)
            y += 20f
        }

        // Totals
        y = 700f
        canvas.drawLine(350f, y, 555f, y, Paint().apply { strokeWidth = 1f; color = 0xFFCCCCCC.toInt() }); y += 20f
        canvas.drawText("Subtotal", 360f, y, bodyPaint)
        canvas.drawText(money(invoice.amount), 480f, y, bodyPaint); y += 24f
        
        val totalPaint = Paint().apply { textSize = 16f; isFakeBoldText = true; color = 0xFF1A237E.toInt() }
        canvas.drawText("Total", 360f, y, totalPaint)
        canvas.drawText(money(invoice.amount), 480f, y, totalPaint); y += 60f

        // Footer
        val footerText = "This is a system-generated document and does not require a physical signature for validity."
        canvas.drawText(footerText, pageWidth / 2f, pageHeight - 50f, footerPaint)

        doc.finishPage(page)
        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, "Invoice_${invoice.invoiceNumber}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun exportParty(context: Context, businessName: String, party: Party, entries: List<LedgerEntry>): File {
        val pageWidth = 595
        val pageHeight = 842
        val doc = PdfDocument()
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        var page = doc.startPage(pageInfo)
        var canvas = page.canvas
        var y = 40f

        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val subPaint = Paint().apply { textSize = 11f; color = 0xFF666666.toInt() }
        val headerPaint = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 10.5f }
        val greenPaint = Paint().apply { textSize = 12f; isFakeBoldText = true; color = 0xFF1F4D3D.toInt() }
        val redPaint = Paint().apply { textSize = 12f; isFakeBoldText = true; color = 0xFFA4332B.toInt() }

        canvas.drawText(businessName, 40f, y, titlePaint); y += 22f
        canvas.drawText("Statement for: ${party.name}", 40f, y, subPaint); y += 16f
        canvas.drawText("Phone: ${party.phone.ifBlank { "-" }}", 40f, y, subPaint); y += 16f
        canvas.drawText("Generated: ${dateFmt.format(Date())}", 40f, y, subPaint); y += 24f

        val bal = balanceOf(entries)
        val balLabel = if (bal > 0) "You will get ${money(bal)}" else if (bal < 0) "You will give ${money(bal)}" else "Settled up"
        canvas.drawText("Current balance: $balLabel", 40f, y, if (bal >= 0) greenPaint else redPaint)
        y += 28f

        val colDate = 40f; val colType = 150f; val colNote = 230f; val colAmt = 400f; val colBal = 480f
        canvas.drawText("Date", colDate, y, headerPaint)
        canvas.drawText("Type", colType, y, headerPaint)
        canvas.drawText("Note", colNote, y, headerPaint)
        canvas.drawText("Amount", colAmt, y, headerPaint)
        canvas.drawText("Balance", colBal, y, headerPaint)
        y += 8f
        canvas.drawLine(40f, y, 555f, y, Paint().apply { strokeWidth = 1f; color = 0xFFCCCCCC.toInt() })
        y += 16f

        var running = 0.0
        val sorted = entries.sortedBy { it.timestamp }
        for (e in sorted) {
            running += if (e.type == "GET") e.amount else -e.amount
            if (y > 800f) {
                doc.finishPage(page)
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, doc.pages.size + 1).create()
                page = doc.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
            }
            canvas.drawText(dateFmt.format(Date(e.timestamp)), colDate, y, bodyPaint)
            canvas.drawText(if (e.type == "GET") "You Got" else "You Gave", colType, y, bodyPaint)
            canvas.drawText(e.note.ifBlank { "-" }.take(28), colNote, y, bodyPaint)
            canvas.drawText((if (e.type == "GET") "+" else "-") + money(e.amount), colAmt, y, bodyPaint)
            canvas.drawText(money(running), colBal, y, bodyPaint)
            y += 20f
        }
        doc.finishPage(page)

        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, "${party.name.replace(Regex("\\s+"), "_")}_statement.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    fun exportAll(context: Context, businessName: String, parties: List<Party>, entriesByParty: Map<String, List<LedgerEntry>>): File {
        val pageWidth = 595; val pageHeight = 842
        val doc = PdfDocument()
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        var page = doc.startPage(pageInfo)
        var canvas = page.canvas
        var y = 40f

        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val subPaint = Paint().apply { textSize = 11f; color = 0xFF666666.toInt() }
        val nameP = Paint().apply { textSize = 12f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 9.5f }

        canvas.drawText(businessName, 40f, y, titlePaint); y += 22f
        canvas.drawText("Full Ledger Report — ${dateFmt.format(Date())}", 40f, y, subPaint); y += 26f

        var totalGet = 0.0; var totalGive = 0.0
        for (p in parties) {
            val b = balanceOf(entriesByParty[p.firestoreId] ?: emptyList())
            if (b > 0) totalGet += b else totalGive += -b
        }
        canvas.drawText("Total you will get: ${money(totalGet)}", 40f, y, nameP); y += 16f
        canvas.drawText("Total you will give: ${money(totalGive)}", 40f, y, nameP); y += 26f

        for (p in parties) {
            val entries = (entriesByParty[p.firestoreId] ?: emptyList()).sortedBy { it.timestamp }
            val bal = balanceOf(entries)
            if (y > 780f) {
                doc.finishPage(page)
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, doc.pages.size + 1).create()
                page = doc.startPage(pageInfo); canvas = page.canvas; y = 40f
            }
            canvas.drawText("${p.name}  -  ${money(bal)} ${if (bal >= 0) "(get)" else "(give)"}", 40f, y, nameP)
            y += 18f
            var running = 0.0
            for (e in entries) {
                running += if (e.type == "GET") e.amount else -e.amount
                if (y > 800f) {
                    doc.finishPage(page)
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, doc.pages.size + 1).create()
                    page = doc.startPage(pageInfo); canvas = page.canvas; y = 40f
                }
                canvas.drawText(
                    "${dateFmt.format(Date(e.timestamp))}  ${if (e.type == "GET") "Got" else "Gave"}  ${e.note.take(24)}  ${money(e.amount)}  Bal:${money(running)}",
                    56f, y, bodyPaint
                )
                y += 14f
            }
            y += 14f
        }
        doc.finishPage(page)

        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, "full_ledger_report.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
