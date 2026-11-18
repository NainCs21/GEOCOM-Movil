package com.example.apimovil.ui.common

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class FilaReportePdf(
    val entidad: String,
    val cantidadOrdenes: Int,
    val montoTotal: Double
)

suspend fun generarReportePdf(
    context: Context,
    tituloReporte: String,
    textoPeriodo: String,
    nombreGenerador: String?,
    nombreColumnaEntidad: String,
    filas: List<FilaReportePdf>,
    totalOrdenes: Int,
    montoTotalGeneral: Double,
    esConductor: Boolean,
    desde: String?,
    hasta: String?
): File? = withContext(Dispatchers.IO) {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4

    val paintTitle = Paint().apply {
        color = Color.parseColor("#24212D")
        textSize = 16f
        isFakeBoldText = true
    }
    val paintSubtitle = Paint().apply {
        color = Color.parseColor("#666270")
        textSize = 11f
    }
    val paintHeader = Paint().apply {
        color = Color.parseColor("#24212D")
        textSize = 10f
    }
    val paintTableHeader = Paint().apply {
        color = Color.parseColor("#6042A6")
    }
    val paintHeaderText = Paint().apply {
        color = Color.WHITE
        textSize = 10f
        isFakeBoldText = true
    }
    val paintRowText = TextPaint().apply {
        color = Color.parseColor("#24212D")
        textSize = 10f
    }
    val paintRowLine = Paint().apply {
        color = Color.parseColor("#E5E0EB")
        strokeWidth = 1f
    }
    val paintTotalText = Paint().apply {
        color = Color.parseColor("#24212D")
        textSize = 10f
        isFakeBoldText = true
    }
    val paintFooter = Paint().apply {
        color = Color.parseColor("#666270")
        textSize = 9f
        textAlign = Paint.Align.CENTER
    }

    val marginLeft = 40f
    val marginRight = 40f
    val pageWidth = 595f
    val pageHeight = 842f
    val contentWidth = pageWidth - marginLeft - marginRight

    val colNoWidth = 35f
    val colOrdenesWidth = 75f
    val colMontoWidth = 85f
    val colEntidadWidth = contentWidth - colNoWidth - colOrdenesWidth - colMontoWidth

    val xNo = marginLeft
    val xEntidad = xNo + colNoWidth
    val xOrdenes = xEntidad + colEntidadWidth
    val xMonto = xOrdenes + colOrdenesWidth

    val rowHeight = 22f
    val headerHeight = 24f

    val maxRowsFirstPage = ((pageHeight - 90 - 50 - headerHeight - 30) / rowHeight).toInt().coerceAtLeast(1)
    val maxRowsOtherPages = ((pageHeight - 50 - headerHeight - 40 - 30) / rowHeight).toInt().coerceAtLeast(1)

    val paginasFilas = mutableListOf<List<IndexedValue<FilaReportePdf>>>()
    val indexedRows = filas.withIndex().toList()
    if (indexedRows.isEmpty()) {
        paginasFilas.add(emptyList())
    } else {
        var index = 0
        while (index < indexedRows.size) {
            val limit = if (paginasFilas.isEmpty()) maxRowsFirstPage else maxRowsOtherPages
            val end = (index + limit).coerceAtMost(indexedRows.size)
            paginasFilas.add(indexedRows.subList(index, end))
            index = end
        }
    }

    val totalPages = paginasFilas.size
    val fechaGen = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "HN")).format(Date())

    try {
        for ((pageIndex, pageRows) in paginasFilas.withIndex()) {
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            var currentY = 50f

            if (pageIndex == 0) {
                canvas.drawText("GEOCOM · Sistema de Órdenes de Combustible", marginLeft, currentY, paintHeader)
                currentY += 20f

                canvas.drawText(tituloReporte, marginLeft, currentY, paintTitle)
                currentY += 20f

                val periodoStr = if (textoPeriodo.isBlank()) "Órdenes completadas, todo el periodo" else "Órdenes completadas del $textoPeriodo"
                canvas.drawText(periodoStr, marginLeft, currentY, paintSubtitle)
                currentY += 16f

                val genStr = "Generado el $fechaGen por ${nombreGenerador ?: "Administrador"}"
                canvas.drawText(genStr, marginLeft, currentY, paintSubtitle)
                currentY += 25f
            } else {
                canvas.drawText(tituloReporte, marginLeft, currentY, paintHeader)
                currentY += 20f
            }

            canvas.drawRect(marginLeft, currentY, pageWidth - marginRight, currentY + headerHeight, paintTableHeader)
            val textBaselineHeader = currentY + 16f
            canvas.drawText("#", xNo + 8f, textBaselineHeader, paintHeaderText)
            canvas.drawText(nombreColumnaEntidad, xEntidad + 8f, textBaselineHeader, paintHeaderText)
            canvas.drawText("Órdenes", xOrdenes + 8f, textBaselineHeader, paintHeaderText)
            canvas.drawText("Monto (L)", xMonto + 8f, textBaselineHeader, paintHeaderText)
            currentY += headerHeight

            for ((originalIndex, row) in pageRows) {
                canvas.drawLine(marginLeft, currentY, pageWidth - marginRight, currentY, paintRowLine)
                val textBaselineRow = currentY + 15f

                canvas.drawText((originalIndex + 1).toString(), xNo + 8f, textBaselineRow, paintRowText)

                val entidadEllipsized = TextUtils.ellipsize(
                    row.entidad,
                    paintRowText,
                    colEntidadWidth - 16f,
                    TextUtils.TruncateAt.END
                ).toString()
                canvas.drawText(entidadEllipsized, xEntidad + 8f, textBaselineRow, paintRowText)

                canvas.drawText(row.cantidadOrdenes.toString(), xOrdenes + 8f, textBaselineRow, paintRowText)

                val montoStr = String.format(Locale.US, "%,.2f", row.montoTotal)
                val montoWidth = paintRowText.measureText(montoStr)
                canvas.drawText(montoStr, (xMonto + colMontoWidth - 8f) - montoWidth, textBaselineRow, paintRowText)

                currentY += rowHeight
            }

            if (pageIndex == totalPages - 1) {
                canvas.drawLine(marginLeft, currentY, pageWidth - marginRight, currentY, paintRowLine)
                currentY += 4f
                val textBaselineTotal = currentY + 12f
                canvas.drawText("Total general", xNo + 8f, textBaselineTotal, paintTotalText)
                canvas.drawText(totalOrdenes.toString(), xOrdenes + 8f, textBaselineTotal, paintTotalText)

                val totalGeneralStr = String.format(Locale.US, "%,.2f", montoTotalGeneral)
                val totalWidth = paintTotalText.measureText(totalGeneralStr)
                canvas.drawText(totalGeneralStr, (xMonto + colMontoWidth - 8f) - totalWidth, textBaselineTotal, paintTotalText)
            }

            val footerText = "Página ${pageIndex + 1} de $totalPages"
            canvas.drawText(footerText, pageWidth / 2f, pageHeight - 25f, paintFooter)

            document.finishPage(page)
        }

        val dir = File(context.cacheDir, "reportes")
        if (!dir.exists()) dir.mkdirs()

        val tipoPrefijo = if (esConductor) "reporte_conductor" else "reporte_gasolinera"
        val periodoLimpio = if (desde.isNullOrBlank() || hasta.isNullOrBlank()) {
            "todo_el_periodo"
        } else {
            "${desde}_a_$hasta"
        }
        val fileName = "${tipoPrefijo}_$periodoLimpio.pdf"
        val file = File(dir, fileName)

        val fos = FileOutputStream(file)
        document.writeTo(fos)
        fos.close()
        document.close()
        file
    } catch (_: Exception) {
        try {
            document.close()
        } catch (_: Exception) {}
        null
    }
}
