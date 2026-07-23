package com.myfinancialbook.app.export

import android.content.Context
import com.myfinancialbook.app.data.LedgerEntry
import com.myfinancialbook.app.data.Party
import com.myfinancialbook.app.data.balanceOf
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Lightweight XLSX writer that produces valid Excel-compatible spreadsheets
 * without needing Apache POI (keeps the app lightweight on Android).
 * Supports one or more sheets, each a list of rows of string/number cells.
 */
object XlsxExporter {

    private val dateFmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

    data class Sheet(val name: String, val header: List<String>, val rows: List<List<Any>>)

    fun write(context: Context, fileName: String, sheets: List<Sheet>): File {
        val dir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val file = File(dir, fileName)

        ZipOutputStream(file.outputStream()).use { zos ->
            entry(zos, "[Content_Types].xml", contentTypesXml(sheets.size))
            entry(zos, "_rels/.rels", relsXml())
            entry(zos, "xl/workbook.xml", workbookXml(sheets))
            entry(zos, "xl/_rels/workbook.xml.rels", workbookRelsXml(sheets.size))
            sheets.forEachIndexed { idx, sheet ->
                entry(zos, "xl/worksheets/sheet${idx + 1}.xml", sheetXml(sheet))
            }
        }
        return file
    }

    private fun entry(zos: ZipOutputStream, name: String, content: String) {
        zos.putNextEntry(ZipEntry(name))
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun esc(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&apos;")

    private fun contentTypesXml(sheetCount: Int): String {
        val overrides = (1..sheetCount).joinToString("") {
            "<Override PartName=\"/xl/worksheets/sheet$it.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
$overrides
</Types>"""
    }

    private fun relsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private fun workbookXml(sheets: List<Sheet>): String {
        val sheetTags = sheets.mapIndexed { i, s ->
            "<sheet name=\"${esc(s.name)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>"
        }.joinToString("")
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets>$sheetTags</sheets>
</workbook>"""
    }

    private fun workbookRelsXml(sheetCount: Int): String {
        val rels = (1..sheetCount).joinToString("") {
            "<Relationship Id=\"rId$it\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet$it.xml\"/>"
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$rels
</Relationships>"""
    }

    private fun colLetter(n: Int): String {
        var num = n; val sb = StringBuilder()
        while (num >= 0) { sb.insert(0, ('A' + (num % 26))); num = num / 26 - 1 }
        return sb.toString()
    }

    private fun sheetXml(sheet: Sheet): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")

        fun rowXml(rowIdx: Int, values: List<Any>): String {
            val cells = values.mapIndexed { ci, v ->
                val ref = "${colLetter(ci)}${rowIdx + 1}"
                when (v) {
                    is Number -> "<c r=\"$ref\"><v>${v}</v></c>"
                    else -> "<c r=\"$ref\" t=\"inlineStr\"><is><t>${esc(v.toString())}</t></is></c>"
                }
            }.joinToString("")
            return "<row r=\"${rowIdx + 1}\">$cells</row>"
        }

        sb.append(rowXml(0, sheet.header))
        sheet.rows.forEachIndexed { i, row -> sb.append(rowXml(i + 1, row)) }
        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    // ---- convenience builders for this app ----

    fun exportParty(context: Context, party: Party, entries: List<LedgerEntry>): File {
        var running = 0.0
        val rows = entries.sortedBy { it.timestamp }.map { e ->
            running += if (e.type == "GET") e.amount else -e.amount
            listOf(
                dateFmt.format(Date(e.timestamp)),
                if (e.type == "GET") "You Got" else "You Gave",
                e.note.ifBlank { "-" },
                e.amount,
                running
            )
        }
        val sheet = Sheet("Statement", listOf("Date", "Type", "Note", "Amount", "Running Balance"), rows)
        return write(context, "${party.name.replace(Regex("\\s+"), "_")}_statement.xlsx", listOf(sheet))
    }

    fun exportAll(context: Context, parties: List<Party>, entriesByParty: Map<String, List<LedgerEntry>>): File {
        val summaryRows = parties.map { p ->
            val bal = balanceOf(entriesByParty[p.firestoreId] ?: emptyList())
            listOf(
                p.name, p.phone, bal,
                if (bal > 0) "You will get" else if (bal < 0) "You will give" else "Settled"
            )
        }
        val summarySheet = Sheet("Summary", listOf("Party", "Phone", "Balance", "Status"), summaryRows)

        val partySheets = parties.map { p ->
            var running = 0.0
            val rows = (entriesByParty[p.firestoreId] ?: emptyList()).sortedBy { it.timestamp }.map { e ->
                running += if (e.type == "GET") e.amount else -e.amount
                listOf(
                    dateFmt.format(Date(e.timestamp)),
                    if (e.type == "GET") "You Got" else "You Gave",
                    e.note.ifBlank { "-" },
                    e.amount,
                    running
                )
            }
            val safeName = p.name.take(28).replace(Regex("[\\\\/?*\\[\\]:]"), "").ifBlank { "Party" }
            Sheet(safeName, listOf("Date", "Type", "Note", "Amount", "Running Balance"), rows)
        }

        return write(context, "full_ledger_report.xlsx", listOf(summarySheet) + partySheets)
    }
}
