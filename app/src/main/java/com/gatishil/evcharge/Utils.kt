package com.gatishil.evcharge

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {
    fun generateReceipt(context: Context, tx: Transaction): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 0).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("Gatishil Yatayat Pvt. Ltd.", 50f, 50f, paint)
        
        paint.textSize = 18f
        canvas.drawText("EV Fast Charging Receipt", 50f, 80f, paint)
        
        paint.textSize = 14f
        paint.isFakeBoldText = false
        var y = 130f
        val lineHeight = 30f

        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val details = listOf(
            "Receipt No: ${tx.txNumber}",
            "Date: ${sdf.format(Date(tx.timestamp))}",
            "Vehicle Model: ${tx.vehicleModel}",
            "Vehicle No: ${tx.vehicleNo}",
            "Start Battery: ${tx.startBattery}%",
            "End Battery: ${tx.endBattery}%",
            "Charged Battery: ${tx.chargedBattery}%",
            "Rate: NPR ${tx.rateUsed} per %",
            "Total Bill: NPR ${tx.billAmount}",
            "Payment Mode: ${tx.paymentMode}"
        )

        for (line in details) {
            canvas.drawText(line, 50f, y, paint)
            y += lineHeight
        }
        
        canvas.drawText("Thank you for charging with us!", 50f, y + 50f, paint)

        document.finishPage(page)
        
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Receipts")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Receipt_${tx.txNumber}.pdf")
        
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Receipt via"))
    }
    
    fun exportCsv(context: Context, transactions: List<Transaction>, dateStr: String): File {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Reports")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "Daily_Summary_$dateStr.csv")
        
        file.printWriter().use { out ->
            out.println("S.N.,Transaction No.,Time,Vehicle Model,Vehicle No.,Start %,End %,Charged %,Bill Amount (NPR),Payment Mode")
            transactions.forEachIndexed { index, tx ->
                val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
                out.println("${index + 1},${tx.txNumber},$time,${tx.vehicleModel},${tx.vehicleNo},${tx.startBattery},${tx.endBattery},${tx.chargedBattery},${tx.billAmount},${tx.paymentMode}")
            }
        }
        return file
    }
}
