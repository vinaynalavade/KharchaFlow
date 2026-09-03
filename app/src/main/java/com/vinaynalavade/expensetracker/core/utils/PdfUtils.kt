package com.vinaynalavade.expensetracker.core.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.vinaynalavade.expensetracker.core.constants.AppConstants
import com.vinaynalavade.expensetracker.domain.model.StatementReport
import com.vinaynalavade.expensetracker.domain.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * On-device PDF generator for professional Leaf financial statements.
 * Zero external libraries or internet connections required.
 */
object PdfUtils {

    fun generateStatementPdf(context: Context, report: StatementReport): File {
        val pdfDocument = PdfDocument()
        val pageWidth = 595 // A4 standard width at 72dpi
        val pageHeight = 842 // A4 standard height at 72dpi

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val margin = 36f
        var currentY = margin + 20f

        // 1. Draw Header
        paint.color = Color.rgb(7, 165, 132) // Emerald Teal
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 20f
        textPaint.color = Color.rgb(6, 78, 59)
        canvas.drawText("LEAF", margin, currentY, textPaint)

        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = Color.rgb(100, 116, 139)
        canvas.drawText("Created by ${AppConstants.APP_CREATOR}", margin, currentY + 14f, textPaint)

        textPaint.textSize = 14f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.rgb(15, 23, 42)
        val subtitle = "Financial Statement"
        val subtitleWidth = textPaint.measureText(subtitle)
        canvas.drawText(subtitle, pageWidth - margin - subtitleWidth, currentY, textPaint)

        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val dateText = "Generated on ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())}"
        val dateWidth = textPaint.measureText(dateText)
        canvas.drawText(dateText, pageWidth - margin - dateWidth, currentY + 14f, textPaint)

        currentY += 36f

        // Divider
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1.5f
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint)

        currentY += 20f

        // 2. Period & Currency Context
        textPaint.textSize = 11f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.rgb(79, 70, 229)
        canvas.drawText("Statement Period: ${report.periodTitle}", margin, currentY, textPaint)

        val currText = "Base Currency: ${report.currency.name} (${report.currency.symbol})"
        val currWidth = textPaint.measureText(currText)
        canvas.drawText(currText, pageWidth - margin - currWidth, currentY, textPaint)

        currentY += 20f

        // 3. Summary Box
        paint.color = Color.rgb(248, 250, 252)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(margin, currentY, pageWidth - margin, currentY + 54f, 8f, 8f, paint)

        paint.color = Color.rgb(226, 232, 240)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(margin, currentY, pageWidth - margin, currentY + 54f, 8f, 8f, paint)

        val colWidth = (pageWidth - margin * 2) / 4
        val sumY = currentY + 18f

        fun drawSummaryCol(title: String, amountStr: String, colIdx: Int, color: Int) {
            val colX = margin + colIdx * colWidth + 10f
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = Color.rgb(100, 116, 139)
            canvas.drawText(title.uppercase(), colX, sumY, textPaint)

            textPaint.textSize = 11f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = color
            canvas.drawText(amountStr, colX, sumY + 18f, textPaint)
        }

        drawSummaryCol("Opening Balance", report.openingBalance.format(report.currency), 0, Color.rgb(15, 23, 42))
        drawSummaryCol("Total Income", "+ " + report.totalIncome.format(report.currency), 1, Color.rgb(16, 185, 129))
        drawSummaryCol("Total Expense", "- " + report.totalExpense.format(report.currency), 2, Color.rgb(239, 68, 68))
        drawSummaryCol("Closing Balance", report.closingBalance.format(report.currency), 3, Color.rgb(79, 70, 229))

        currentY += 74f

        // 4. Ledger Table Header
        paint.color = Color.rgb(241, 245, 249)
        paint.style = Paint.Style.FILL
        canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 22f, paint)

        textPaint.textSize = 9f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.color = Color.rgb(71, 85, 105)

        val dateX = margin + 6f
        val descX = margin + 70f
        val catX = margin + 220f
        val inX = margin + 330f
        val outX = margin + 400f
        val balX = margin + 470f

        canvas.drawText("Date", dateX, currentY + 14f, textPaint)
        canvas.drawText("Description", descX, currentY + 14f, textPaint)
        canvas.drawText("Category", catX, currentY + 14f, textPaint)
        canvas.drawText("Income", inX, currentY + 14f, textPaint)
        canvas.drawText("Expense", outX, currentY + 14f, textPaint)
        canvas.drawText("Balance", balX, currentY + 14f, textPaint)

        currentY += 26f

        // 5. Table Rows
        val rowHeight = 22f
        val bottomThreshold = pageHeight - margin - 30f

        report.ledgerItems.forEachIndexed { index, item ->
            if (currentY + rowHeight > bottomThreshold) {
                // Draw Footer for current page
                drawPageFooter(canvas, pageNumber, margin, pageWidth, pageHeight, textPaint)
                pdfDocument.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                currentY = margin + 20f

                // Redraw table header on new page
                paint.color = Color.rgb(241, 245, 249)
                paint.style = Paint.Style.FILL
                canvas.drawRect(margin, currentY, pageWidth - margin, currentY + 22f, paint)

                textPaint.textSize = 9f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textPaint.color = Color.rgb(71, 85, 105)

                canvas.drawText("Date", dateX, currentY + 14f, textPaint)
                canvas.drawText("Description", descX, currentY + 14f, textPaint)
                canvas.drawText("Category", catX, currentY + 14f, textPaint)
                canvas.drawText("Income", inX, currentY + 14f, textPaint)
                canvas.drawText("Expense", outX, currentY + 14f, textPaint)
                canvas.drawText("Balance", balX, currentY + 14f, textPaint)

                currentY += 26f
            }

            // Alternating row background
            if (index % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                paint.style = Paint.Style.FILL
                canvas.drawRect(margin, currentY - 4f, pageWidth - margin, currentY + rowHeight - 6f, paint)
            }

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.textSize = 8.5f
            textPaint.color = Color.rgb(15, 23, 42)

            canvas.drawText(item.dateString, dateX, currentY + 10f, textPaint)

            val cleanDesc = if (item.description.length > 28) item.description.take(25) + "..." else item.description
            canvas.drawText(cleanDesc, descX, currentY + 10f, textPaint)

            val cleanCat = if (item.categoryName.length > 18) item.categoryName.take(15) + "..." else item.categoryName
            canvas.drawText(cleanCat, catX, currentY + 10f, textPaint)

            // Income
            if (item.type == TransactionType.INCOME && item.amount != null) {
                textPaint.color = Color.rgb(16, 185, 129)
                canvas.drawText("+ " + item.amount.format(report.currency, includeSymbol = false), inX, currentY + 10f, textPaint)
            } else {
                textPaint.color = Color.rgb(148, 163, 184)
                canvas.drawText("—", inX, currentY + 10f, textPaint)
            }

            // Expense
            if (item.type == TransactionType.EXPENSE && item.amount != null) {
                textPaint.color = Color.rgb(239, 68, 68)
                canvas.drawText("- " + item.amount.format(report.currency, includeSymbol = false), outX, currentY + 10f, textPaint)
            } else {
                textPaint.color = Color.rgb(148, 163, 184)
                canvas.drawText("—", outX, currentY + 10f, textPaint)
            }

            // Running Balance
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.color = Color.rgb(15, 23, 42)
            canvas.drawText(item.runningBalance.format(report.currency), balX, currentY + 10f, textPaint)

            currentY += rowHeight
        }

        drawPageFooter(canvas, pageNumber, margin, pageWidth, pageHeight, textPaint)
        pdfDocument.finishPage(page)

        val statementsDir = File(context.cacheDir, "statements").apply { mkdirs() }
        val fileName = "ExpenseTracker_Statement_${System.currentTimeMillis()}.pdf"
        val outputFile = File(statementsDir, fileName)

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int, margin: Float, pageWidth: Int, pageHeight: Int, textPaint: Paint) {
        val footerY = pageHeight - margin + 10f
        textPaint.textSize = 8f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textPaint.color = Color.rgb(148, 163, 184)

        canvas.drawText("Leaf • Personal Financial Statement", margin, footerY, textPaint)

        val pageText = "Page $pageNumber"
        val pageTextWidth = textPaint.measureText(pageText)
        canvas.drawText(pageText, pageWidth - margin - pageTextWidth, footerY, textPaint)
    }

    fun createShareIntent(context: Context, pdfFile: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Leaf Statement")
            putExtra(Intent.EXTRA_TEXT, "Here is my Leaf financial statement.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
