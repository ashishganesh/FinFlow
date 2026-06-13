package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportManager {

    /**
     * Shares a local file using safe Android FileProvider file-sharing.
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    /**
     * Generates a beautifully-styled, professional financial ledger PDF report.
     * Uses android.graphics.pdf.PdfDocument to paint clean cards, charts, and tables.
     */
    fun generatePdfReport(
        context: Context,
        accountName: String,
        dateRangeStr: String,
        openingBalance: Double,
        closingBalance: Double,
        totalIncome: Double,
        totalExpenses: Double,
        remainingBalance: Double,
        carryForward: Double,
        transactions: List<Transaction>,
        categorySummary: Map<String, Double>,
        currencySymbol: String
    ): File? {
        val document = PdfDocument()

        // Page size metrics (Standard A4 is 595 x 842 points)
        val pageWidth = 595
        val pageHeight = 842

        // Setup paint configurations
        val textPaint = Paint().apply { isAntiAlias = true }
        val fillPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
        val strokePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE }

        // --- PAGE 1: EXECUTIVE SUMMARY & DASHBOARD METRICS ---
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        // Palette definitions (Premium Dark purple vibe matching our theme)
        val bgPrimary = 0xFF140822.toInt() // Deep background
        val panelBg = 0xFF211135.toInt() // Dark Purple card background
        val accentColor = 0xFFCD9BFF.toInt() // Neon light purple accent
        val textPrimary = 0xFFFFFFFF.toInt() // White text
        val textSecondary = 0xFFB1A0C4.toInt() // Soft lavender text
        val greenIncome = 0xFF0D9488.toInt() // Teal income green
        val redExpense = 0xFFE11D48.toInt() // Rose expense red

        // 1. Draw Page Background
        canvas.drawColor(bgPrimary)

        // 2. Beautiful Header Banner
        fillPaint.color = panelBg
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 130f, fillPaint)

        strokePaint.color = accentColor
        strokePaint.strokeWidth = 2f
        canvas.drawLine(0f, 130f, pageWidth.toFloat(), 130f, strokePaint)

        // Header Title
        textPaint.color = textPrimary
        textPaint.textSize = 22f
        textPaint.isFakeBoldText = true
        canvas.drawText("FINANCIAL STATEMENT", 30f, 50f, textPaint)

        textPaint.color = accentColor
        textPaint.textSize = 10f
        textPaint.isFakeBoldText = false
        canvas.drawText("LEDGER REPORT CO-PILOT", 30f, 70f, textPaint)

        // Date and Account Information
        textPaint.color = textPrimary
        textPaint.textSize = 12f
        textPaint.isFakeBoldText = true
        canvas.drawText("Profile: $accountName", 350f, 45f, textPaint)

        textPaint.color = textSecondary
        textPaint.textSize = 10f
        textPaint.isFakeBoldText = false
        canvas.drawText("Statement Range: $dateRangeStr", 350f, 65f, textPaint)
        
        val timestampLabel = SimpleDateFormat("MMM d, yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $timestampLabel", 350f, 82f, textPaint)

        // 3. Overview Cards (Opening, Income, Expense, Closing)
        val cardWidth = 120f
        val cardHeight = 70f
        val cardY = 150f

        val kpiList = listOf(
            Triple("Opening Bal", openingBalance, textSecondary),
            Triple("Total Inflow", totalIncome, greenIncome),
            Triple("Total Outflow", totalExpenses, redExpense),
            Triple("Closing Bal", closingBalance, accentColor)
        )

        for (i in kpiList.indices) {
            val (label, valAmt, cardColor) = kpiList[i]
            val cardX = 30f + i * (cardWidth + 15f)
            
            // Draw card background
            fillPaint.color = panelBg
            canvas.drawRoundRect(RectF(cardX, cardY, cardX + cardWidth, cardY + cardHeight), 12f, 12f, fillPaint)
            
            strokePaint.color = cardColor
            strokePaint.strokeWidth = 1f
            canvas.drawRoundRect(RectF(cardX, cardY, cardX + cardWidth, cardY + cardHeight), 12f, 12f, strokePaint)

            // Draw text labels
            textPaint.color = textSecondary
            textPaint.textSize = 9f
            textPaint.isFakeBoldText = false
            canvas.drawText(label, cardX + 12f, cardY + 22f, textPaint)

            textPaint.color = textPrimary
            textPaint.textSize = 13f
            textPaint.isFakeBoldText = true
            canvas.drawText("$currencySymbol${String.format("%.2f", valAmt)}", cardX + 12f, cardY + 48f, textPaint)
        }

        // Additional Carry-Forward Info Card
        val infoCardY = 240f
        fillPaint.color = panelBg
        canvas.drawRoundRect(RectF(30f, infoCardY, (pageWidth - 30).toFloat(), infoCardY + 48f), 12f, 12f, fillPaint)
        strokePaint.color = accentColor
        strokePaint.strokeWidth = 1f
        canvas.drawRoundRect(RectF(30f, infoCardY, (pageWidth - 30).toFloat(), infoCardY + 48f), 12f, 12f, strokePaint)

        textPaint.color = textSecondary
        textPaint.textSize = 10f
        textPaint.isFakeBoldText = false
        canvas.drawText("Net Balance Saved in Period / Carry Forward Balance:", 45f, infoCardY + 18f, textPaint)

        textPaint.color = if (carryForward >= 0) android.graphics.Color.GREEN else android.graphics.Color.RED
        textPaint.textSize = 14f
        textPaint.isFakeBoldText = true
        val carrySig = if (carryForward >= 0) "+" else ""
        canvas.drawText("$carrySig$currencySymbol${String.format("%.2f", carryForward)}", 45f, infoCardY + 36f, textPaint)

        // 4. Category-Wise Distribution Chart / Summary Table
        val summaryY = 310f
        textPaint.color = textPrimary
        textPaint.textSize = 14f
        textPaint.isFakeBoldText = true
        canvas.drawText("Category Expenditure Insights", 30f, summaryY, textPaint)

        // Draw an elegant bar chart directly on Canvas!
        var currentY = summaryY + 20f
        val sortedSummary = categorySummary.toList().sortedByDescending { it.second }.take(5)
        val maxExpense = sortedSummary.maxOfOrNull { it.second } ?: 1.0

        if (sortedSummary.isEmpty()) {
            textPaint.color = textSecondary
            textPaint.textSize = 11f
            textPaint.isFakeBoldText = false
            canvas.drawText("No spending registered in this period.", 45f, currentY + 30f, textPaint)
        } else {
            for (i in sortedSummary.indices) {
                val (catName, amt) = sortedSummary[i]
                
                // Name
                textPaint.color = textPrimary
                textPaint.textSize = 10f
                textPaint.isFakeBoldText = true
                canvas.drawText(catName, 45f, currentY + 15f, textPaint)

                // Draw spending progress bar track
                fillPaint.color = 0xFF2D1E40.toInt()
                canvas.drawRoundRect(RectF(150f, currentY + 5f, 420f, currentY + 16f), 6f, 6f, fillPaint)

                // Fill actual spending part
                val progressWidth = ((amt / maxExpense) * 270f).toFloat()
                fillPaint.color = accentColor
                canvas.drawRoundRect(RectF(150f, currentY + 5f, 150f + progressWidth, currentY + 16f), 6f, 6f, fillPaint)

                // Amount text
                textPaint.color = textPrimary
                textPaint.textSize = 10f
                canvas.drawText("$currencySymbol${String.format("%.2f", amt)}", 440f, currentY + 15f, textPaint)

                currentY += 28f
            }
        }

        // Section header for transaction list
        val txSectionHeaderY = 510f
        textPaint.color = textPrimary
        textPaint.textSize = 14f
        textPaint.isFakeBoldText = true
        canvas.drawText("Detailed Transaction Records", 30f, txSectionHeaderY, textPaint)

        // Finish Page 1
        document.finishPage(page)

        // --- PAGE 2 & ONWARD: TRANSACTION LISTING TABLE ---
        val transPerPage = 18
        val itemHeight = 35f
        var transactionIndex = 0

        while (transactionIndex < transactions.size) {
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas

            // Page Background
            canvas.drawColor(bgPrimary)

            // Dynamic Page Header Bar
            fillPaint.color = panelBg
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 60f, fillPaint)
            strokePaint.color = accentColor
            strokePaint.strokeWidth = 1f
            canvas.drawLine(0f, 60f, pageWidth.toFloat(), 60f, strokePaint)

            textPaint.color = textPrimary
            textPaint.textSize = 11f
            textPaint.isFakeBoldText = true
            canvas.drawText("LEDGER: $accountName (${dateRangeStr})", 30f, 35f, textPaint)
            textPaint.color = textSecondary
            canvas.drawText("Page $pageNumber", 500f, 35f, textPaint)

            // Setup Table Column Headers
            val startY = 90f
            fillPaint.color = 0xFF2D174A.toInt() // Dark header
            canvas.drawRect(30f, startY, (pageWidth - 30).toFloat(), startY + 28f, fillPaint)

            textPaint.color = textPrimary
            textPaint.textSize = 10f
            textPaint.isFakeBoldText = true
            
            canvas.drawText("DATE", 40f, startY + 18f, textPaint)
            canvas.drawText("TITLE & REMARKS", 130f, startY + 18f, textPaint)
            canvas.drawText("CATEGORY", 320f, startY + 18f, textPaint)
            canvas.drawText("METHOD", 420f, startY + 18f, textPaint)
            canvas.drawText("AMOUNT", 495f, startY + 18f, textPaint)

            var rowY = startY + 28f
            val maxRowOnThisPage = if (pageNumber == 2) 18 else 19

            var rowsDrawn = 0
            while (rowsDrawn < maxRowOnThisPage && transactionIndex < transactions.size) {
                val tx = transactions[transactionIndex]
                
                // Draw zebra row stripe
                if (rowsDrawn % 2 == 1) {
                    fillPaint.color = 0xFF1C0E2D.toInt()
                    canvas.drawRect(30f, rowY, (pageWidth - 30).toFloat(), rowY + itemHeight, fillPaint)
                }

                // Row border bottom path
                strokePaint.color = 0xFF2F1D45.toInt()
                strokePaint.strokeWidth = 0.5f
                canvas.drawLine(30f, rowY + itemHeight, (pageWidth - 30).toFloat(), rowY + itemHeight, strokePaint)

                // Date
                val txDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(tx.timestamp))
                textPaint.color = textSecondary
                textPaint.textSize = 9f
                textPaint.isFakeBoldText = false
                canvas.drawText(txDate, 40f, rowY + 22f, textPaint)

                // Title & Remarks
                textPaint.color = textPrimary
                textPaint.isFakeBoldText = true
                val dispTitle = if (tx.title.length > 25) tx.title.take(22) + "..." else tx.title
                canvas.drawText(dispTitle, 130f, rowY + 18f, textPaint)

                // Subtext Remarks
                textPaint.color = textSecondary
                textPaint.isFakeBoldText = false
                textPaint.textSize = 8f
                val dispRemarks = if (tx.remarks.isNotBlank()) {
                    if (tx.remarks.length > 30) tx.remarks.take(27) + "..." else tx.remarks
                } else {
                    "No description"
                }
                canvas.drawText(dispRemarks, 130f, rowY + 29f, textPaint)

                // Category
                textPaint.color = textPrimary
                textPaint.textSize = 9f
                canvas.drawText(tx.category, 320f, rowY + 21f, textPaint)

                // Payment Mode
                textPaint.color = textSecondary
                canvas.drawText(tx.paymentMode, 420f, rowY + 21f, textPaint)

                // Amount with color indicator
                val amountText = "${if (tx.type == "INCOME") "+" else "-"}$currencySymbol${String.format("%.2f", tx.amount)}"
                textPaint.color = if (tx.type == "INCOME") greenIncome else redExpense
                textPaint.isFakeBoldText = true
                canvas.drawText(amountText, 495f, rowY + 21f, textPaint)

                rowY += itemHeight
                rowsDrawn++
                transactionIndex++
            }

            document.finishPage(page)
        }

        // Write outputs
        return try {
            val dateSuffix = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val file = File(context.cacheDir, "FinTrackerPro_Report_$dateSuffix.pdf")
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            document.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            null
        }
    }

    /**
     * Sourced helper to export transactions list as a robust, structured Excel compatible XLS format.
     */
    fun generateExcelReport(
        context: Context,
        accountName: String,
        dateRangeStr: String,
        transactions: List<Transaction>,
        accountMap: Map<Int, String>
    ): File? {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val xlsContent = StringBuilder()

        // Write beautiful tab-separated content with metadata that Excel natively translates with styling
        xlsContent.append("EXPENSE & INCOME REPORT GENERATOR\n")
        xlsContent.append("Profile Workspace: $accountName\n")
        xlsContent.append("Report Date Interval: $dateRangeStr\n")
        xlsContent.append("Export Timestamp: ${dateFormat.format(Date())}\n\n")

        // Table headers
        xlsContent.append("Transaction ID\tAccount Location\tAmount\tType\tTitle\tCategory\tTimestamp Interval\tRemarks\tRecurring\tSubscription Interval\tPayment Mode\n")

        for (tx in transactions) {
            val mappedAccountName = accountMap[tx.accountId] ?: "Unassigned Account"
            val formattedDate = dateFormat.format(Date(tx.timestamp))
            val titleEsc = tx.title.replace("\t", " ")
            val remarksEsc = tx.remarks.replace("\t", " ")
            val categoryEsc = tx.category.replace("\t", " ")

            xlsContent.append("${tx.id}\t")
                .append("$mappedAccountName\t")
                .append("${tx.amount}\t")
                .append("${tx.type}\t")
                .append("$titleEsc\t")
                .append("$categoryEsc\t")
                .append("$formattedDate\t")
                .append("$remarksEsc\t")
                .append("${tx.isRecurring}\t")
                .append("${tx.recurringInterval ?: "N/A"}\t")
                .append("${tx.paymentMode}\n")
        }

        return try {
            val dateSuffix = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val file = File(context.cacheDir, "FinTrackerPro_Report_$dateSuffix.xls")
            val outputStream = FileOutputStream(file)
            outputStream.write(xlsContent.toString().toByteArray(Charsets.UTF_8))
            outputStream.flush()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
