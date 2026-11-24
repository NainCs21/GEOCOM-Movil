package com.example.apimovil.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.example.apimovil.data.models.*
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

private fun dibujarBarrasPdf(
    canvas: Canvas, x: Float, y: Float, ancho: Float,
    barras: List<Pair<String, Double>>, horizontal: Boolean, tituloSeccion: String
): Float {
    if (barras.isEmpty()) return y

    val paintTitle = Paint().apply {
        color = Color.parseColor("#24212D")
        textSize = 11f
        isFakeBoldText = true
    }
    val paintBar = Paint().apply {
        color = Color.parseColor("#6042A6")
    }
    val paintText = TextPaint().apply {
        color = Color.parseColor("#24212D")
        textSize = 9f
    }
    val paintLine = Paint().apply {
        color = Color.parseColor("#E5E0EB")
        strokeWidth = 1f
    }

    var currentY = y
    canvas.drawText(tituloSeccion, x, currentY, paintTitle)
    currentY += 14f

    val maxVal = maxOf(barras.maxOfOrNull { it.second } ?: 1.0, 1.0)

    if (horizontal) {
        val rowH = 16f
        val labelW = ancho * 0.38f
        val valW = 55f
        val barMaxW = ancho - labelW - valW - 10f

        for ((etiqueta, valor) in barras) {
            val cy = currentY + rowH / 2f
            val labelEllipsized = TextUtils.ellipsize(etiqueta, paintText, labelW, TextUtils.TruncateAt.END).toString()
            canvas.drawText(labelEllipsized, x, cy + 3f, paintText)

            val barW = (barMaxW * (valor / maxVal)).toFloat().coerceAtLeast(3f)
            val barX = x + labelW + 5f
            val barY = cy - 5f
            canvas.drawRect(barX, barY, barX + barW, barY + 10f, paintBar)

            val valStr = if (valor >= 1000) String.format(Locale.US, "L %.1fk", valor / 1000) else String.format(Locale.US, "L %.0f", valor)
            canvas.drawText(valStr, barX + barW + 5f, cy + 3f, paintText)

            currentY += rowH
        }
    } else {
        val chartH = 90f
        val bottomY = currentY + chartH
        val topY = currentY + 15f
        canvas.drawLine(x, bottomY, x + ancho, bottomY, paintLine)

        val slotW = ancho / barras.size
        val barW = (slotW * 0.6f).coerceAtMost(24f)

        paintText.textAlign = Paint.Align.CENTER
        for ((i, par) in barras.withIndex()) {
            val (etiqueta, valor) = par
            val cx = x + slotW * i + slotW / 2f
            val barH = ((bottomY - topY) * (valor / maxVal)).toFloat()
            val barTop = bottomY - barH

            canvas.drawRect(cx - barW / 2f, barTop, cx + barW / 2f, bottomY, paintBar)

            val valStr = if (valor >= 1000) String.format(Locale.US, "%.1fk", valor / 1000) else String.format(Locale.US, "%.0f", valor)
            canvas.drawText(valStr, cx, barTop - 3f, paintText)

            val labelEllipsized = TextUtils.ellipsize(etiqueta, paintText, slotW - 4f, TextUtils.TruncateAt.END).toString()
            canvas.drawText(labelEllipsized, cx, bottomY + 11f, paintText)
        }
        paintText.textAlign = Paint.Align.LEFT
        currentY = bottomY + 18f
    }

    return currentY + 10f
}

private fun dibujarIndicadoresPdf(canvas: Canvas, x: Float, y: Float, ancho: Float, resumen: ResumenReporte): Float {
    val paintBox = Paint().apply {
        color = Color.parseColor("#F5F3F8")
    }
    val paintText = TextPaint().apply {
        color = Color.parseColor("#24212D")
        textSize = 8.5f
    }
    val paintBold = TextPaint().apply {
        color = Color.parseColor("#6042A6")
        textSize = 9.5f
        isFakeBoldText = true
    }

    val boxH = 32f
    canvas.drawRect(x, y, x + ancho, y + boxH, paintBox)

    val precPromStr = if (resumen.precio_promedio_galon != null) String.format(Locale.US, "L %,.2f", resumen.precio_promedio_galon) else "N/D"
    val galStr = String.format(Locale.US, "≈ %,.1f", resumen.galones_total)

    val colW = ancho / 7f
    val items = listOf(
        "Gasto" to String.format(Locale.US, "L %,.2f", resumen.monto_total),
        "Órdenes" to resumen.cantidad_ordenes.toString(),
        "Promedio" to String.format(Locale.US, "L %,.2f", resumen.promedio_orden),
        "Cumplim." to String.format(Locale.US, "%.1f%%", resumen.cumplimiento_pct),
        "Vencidas" to resumen.vencidas.toString(),
        "Galones" to galStr,
        "Precio prom." to precPromStr
    )

    for ((i, par) in items.withIndex()) {
        val colX = x + colW * i + 4f
        canvas.drawText(par.first, colX, y + 12f, paintText)
        canvas.drawText(par.second, colX, y + 25f, paintBold)
    }

    return y + boxH + 15f
}

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
    hasta: String?,
    resumen: ReporteResumenResp? = null,
    nombreEntidadFiltro: String? = null
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
    val paintFooterNote = Paint().apply {
        color = Color.parseColor("#666270")
        textSize = 8f
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

    val fechaGen = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "HN")).format(Date())
    val tituloReal = if (!nombreEntidadFiltro.isNullOrBlank()) "Reporte de consumo: $nombreEntidadFiltro" else tituloReporte

    try {
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = 50f

        canvas.drawText("GEOCOM · Sistema de Órdenes de Combustible", marginLeft, currentY, paintHeader)
        currentY += 20f

        canvas.drawText(tituloReal, marginLeft, currentY, paintTitle)
        currentY += 20f

        val periodoStr = if (textoPeriodo.isBlank()) "Órdenes completadas, todo el periodo" else "Órdenes completadas del $textoPeriodo"
        canvas.drawText(periodoStr, marginLeft, currentY, paintSubtitle)
        currentY += 16f

        val genStr = "Generado el $fechaGen por ${nombreGenerador ?: "Administrador"}"
        canvas.drawText(genStr, marginLeft, currentY, paintSubtitle)
        currentY += 25f

        if (resumen != null) {
            currentY = dibujarIndicadoresPdf(canvas, marginLeft, currentY, contentWidth, resumen.resumen)
            currentY = dibujarBarrasPdf(canvas, marginLeft, currentY, contentWidth, resumen.por_mes.map { Pair(formatearMes(it.mes), it.monto_total) }, false, "Gasto por mes")
            val rankingList = if (esConductor) resumen.top_conductores.take(8).map { Pair(it.nombre, it.monto_total) }
            else resumen.top_gasolineras.take(8).map { Pair(it.nombre, it.monto_total) }
            val rankingTitle = if (esConductor) "Top conductores" else "Top gasolineras"
            currentY = dibujarBarrasPdf(canvas, marginLeft, currentY, contentWidth, rankingList, true, rankingTitle)
        }

        val indexedRows = filas.withIndex().toList()
        var rowIndex = 0

        if (currentY + headerHeight + rowHeight > pageHeight - 50f) {
            document.finishPage(page)
            page = document.startPage(pageInfo)
            canvas = page.canvas
            currentY = 50f
        }

        canvas.drawRect(marginLeft, currentY, pageWidth - marginRight, currentY + headerHeight, paintTableHeader)
        var textBaselineHeader = currentY + 16f
        canvas.drawText("#", xNo + 8f, textBaselineHeader, paintHeaderText)
        canvas.drawText(nombreColumnaEntidad, xEntidad + 8f, textBaselineHeader, paintHeaderText)
        canvas.drawText("Órdenes", xOrdenes + 8f, textBaselineHeader, paintHeaderText)
        canvas.drawText("Monto (L)", xMonto + 8f, textBaselineHeader, paintHeaderText)
        currentY += headerHeight

        while (rowIndex < indexedRows.size) {
            if (currentY + rowHeight > pageHeight - 50f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = 50f

                canvas.drawRect(marginLeft, currentY, pageWidth - marginRight, currentY + headerHeight, paintTableHeader)
                textBaselineHeader = currentY + 16f
                canvas.drawText("#", xNo + 8f, textBaselineHeader, paintHeaderText)
                canvas.drawText(nombreColumnaEntidad, xEntidad + 8f, textBaselineHeader, paintHeaderText)
                canvas.drawText("Órdenes", xOrdenes + 8f, textBaselineHeader, paintHeaderText)
                canvas.drawText("Monto (L)", xMonto + 8f, textBaselineHeader, paintHeaderText)
                currentY += headerHeight
            }

            val (originalIndex, row) = indexedRows[rowIndex]
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
            rowIndex++
        }

        if (currentY + 25f > pageHeight - 50f) {
            document.finishPage(page)
            page = document.startPage(pageInfo)
            canvas = page.canvas
            currentY = 50f
        }
        canvas.drawLine(marginLeft, currentY, pageWidth - marginRight, currentY, paintRowLine)
        currentY += 4f
        val textBaselineTotal = currentY + 12f
        canvas.drawText("Total general", xNo + 8f, textBaselineTotal, paintTotalText)
        canvas.drawText(totalOrdenes.toString(), xOrdenes + 8f, textBaselineTotal, paintTotalText)

        val totalGeneralStr = String.format(Locale.US, "%,.2f", montoTotalGeneral)
        val totalWidth = paintTotalText.measureText(totalGeneralStr)
        canvas.drawText(totalGeneralStr, (xMonto + colMontoWidth - 8f) - totalWidth, textBaselineTotal, paintTotalText)

        canvas.drawText("Galones y km estimados con el precio oficial vigente al asignar cada orden.", pageWidth / 2f, pageHeight - 25f, paintFooterNote)

        document.finishPage(page)

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

suspend fun generarReporteDetallePdf(
    context: Context,
    tipo: String, // "conductor" o "gasolinera"
    nombre: String,
    subtitulo: String?,
    textoPeriodo: String,
    nombreGenerador: String?,
    resumen: ResumenReporte,
    porMes: List<PuntoMes>,
    porCombustible: List<PuntoCombustible>,
    ranking: List<PuntoRanking>,
    porVehiculo: List<PuntoVehiculo> = emptyList(),
    ordenes: List<OrdenReporte>,
    entidadIdOrNombre: String
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
        textSize = 9f
        isFakeBoldText = true
    }
    val paintRowText = TextPaint().apply {
        color = Color.parseColor("#24212D")
        textSize = 9f
    }
    val paintRowLine = Paint().apply {
        color = Color.parseColor("#E5E0EB")
        strokeWidth = 1f
    }
    val paintTotalText = Paint().apply {
        color = Color.parseColor("#24212D")
        textSize = 9f
        isFakeBoldText = true
    }
    val paintFooterNote = Paint().apply {
        color = Color.parseColor("#666270")
        textSize = 8f
        textAlign = Paint.Align.CENTER
    }

    val marginLeft = 40f
    val marginRight = 40f
    val pageWidth = 595f
    val pageHeight = 842f
    val contentWidth = pageWidth - marginLeft - marginRight

    // Order table columns: # (25), Fecha (60), Destino (165), Comb (75), Galones (65), Monto (125) = 515f
    val colNoW = 25f
    val colFechaW = 60f
    val colCombW = 75f
    val colGalonesW = 65f
    val colMontoW = 125f
    val colDestinoW = contentWidth - colNoW - colFechaW - colCombW - colGalonesW - colMontoW

    val xNo = marginLeft
    val xFecha = xNo + colNoW
    val xDestino = xFecha + colFechaW
    val xComb = xDestino + colDestinoW
    val xGalones = xComb + colCombW
    val xMonto = xGalones + colGalonesW

    val rowHeight = 20f
    val headerHeight = 22f

    val fechaGen = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "HN")).format(Date())
    val tituloReal = "Reporte de consumo: $nombre"

    try {
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var currentY = 50f

        canvas.drawText("GEOCOM · Sistema de Órdenes de Combustible", marginLeft, currentY, paintHeader)
        currentY += 18f

        canvas.drawText(tituloReal, marginLeft, currentY, paintTitle)
        currentY += 18f

        if (!subtitulo.isNullOrBlank()) {
            canvas.drawText(subtitulo, marginLeft, currentY, paintSubtitle)
            currentY += 14f
        }

        val periodoStr = if (textoPeriodo.isBlank()) "Órdenes completadas, todo el periodo" else "Órdenes completadas del $textoPeriodo"
        canvas.drawText(periodoStr, marginLeft, currentY, paintSubtitle)
        currentY += 14f

        val genStr = "Generado el $fechaGen por ${nombreGenerador ?: "Administrador"}"
        canvas.drawText(genStr, marginLeft, currentY, paintSubtitle)
        currentY += 20f

        currentY = dibujarIndicadoresPdf(canvas, marginLeft, currentY, contentWidth, resumen)
        currentY = dibujarBarrasPdf(canvas, marginLeft, currentY, contentWidth, porMes.map { Pair(formatearMes(it.mes), it.monto_total) }, false, "Gasto por mes")
        currentY = dibujarBarrasPdf(canvas, marginLeft, currentY, contentWidth, porCombustible.map {
            Pair(if (it.tipo_combustible.lowercase().contains("gas")) "Gasolina" else "Diésel", it.monto_total)
        }, true, "Gasto por combustible")

        val rankingTitle = if (tipo == "conductor") "Gasolineras donde cargó" else "Conductores que la usan"
        currentY = dibujarBarrasPdf(canvas, marginLeft, currentY, contentWidth, ranking.take(8).map { Pair(it.nombre, it.monto_total) }, true, rankingTitle)

        // Vehículos Table if conductor
        val vehiculosList = porVehiculo
        if (tipo == "conductor" && vehiculosList.isNotEmpty()) {
            val vColPlacaW = 65f
            val vColGastoW = 85f
            val vColGalW = 85f
            val vColKmW = 95f
            val vColDescW = contentWidth - vColPlacaW - vColGastoW - vColGalW - vColKmW

            val vxPlaca = marginLeft
            val vxDesc = vxPlaca + vColPlacaW
            val vxGasto = vxDesc + vColDescW
            val vxGal = vxGasto + vColGastoW
            val vxKm = vxGal + vColGalW

            if (currentY + headerHeight + rowHeight > pageHeight - 50f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = 50f
            }

            canvas.drawText("VEHÍCULOS", marginLeft, currentY, Paint().apply {
                color = Color.parseColor("#24212D")
                textSize = 11f
                isFakeBoldText = true
            })
            currentY += 14f

            canvas.drawRect(marginLeft, currentY, pageWidth - marginRight, currentY + headerHeight, paintTableHeader)
            var textBaselineHeader = currentY + 15f
            canvas.drawText("Placa", vxPlaca + 4f, textBaselineHeader, paintHeaderText)
            canvas.drawText("Descripción", vxDesc + 4f, textBaselineHeader, paintHeaderText)
            canvas.drawText("Gasto (L)", vxGasto + 4f, textBaselineHeader, paintHeaderText)
            canvas.drawText("Galones", vxGal + 4f, textBaselineHeader, paintHeaderText)
            canvas.drawText("Km aprox.", vxKm + 4f, textBaselineHeader, paintHeaderText)
            currentY += headerHeight

            for (pv in vehiculosList) {
                if (currentY + rowHeight > pageHeight - 50f) {
                    document.finishPage(page)
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    currentY = 50f

                    canvas.drawRect(marginLeft, currentY, pageWidth - marginRight, currentY + headerHeight, paintTableHeader)
                    textBaselineHeader = currentY + 15f
                    canvas.drawText("Placa", vxPlaca + 4f, textBaselineHeader, paintHeaderText)
                    canvas.drawText("Descripción", vxDesc + 4f, textBaselineHeader, paintHeaderText)
                    canvas.drawText("Gasto (L)", vxGasto + 4f, textBaselineHeader, paintHeaderText)
                    canvas.drawText("Galones", vxGal + 4f, textBaselineHeader, paintHeaderText)
                    canvas.drawText("Km aprox.", vxKm + 4f, textBaselineHeader, paintHeaderText)
                    currentY += headerHeight
                }

                canvas.drawLine(marginLeft, currentY, pageWidth - marginRight, currentY, paintRowLine)
                val textBaselineRow = currentY + 13f

                canvas.drawText(pv.placa, vxPlaca + 4f, textBaselineRow, paintRowText)

                val descEllipsized = TextUtils.ellipsize(pv.descripcion, paintRowText, vColDescW - 8f, TextUtils.TruncateAt.END).toString()
                canvas.drawText(descEllipsized, vxDesc + 4f, textBaselineRow, paintRowText)

                val gastoStr = String.format(Locale.US, "%,.2f", pv.monto_total)
                canvas.drawText(gastoStr, vxGasto + 4f, textBaselineRow, paintRowText)

                val galStr = String.format(Locale.US, "≈ %.1f", pv.galones_total)
                canvas.drawText(galStr, vxGal + 4f, textBaselineRow, paintRowText)

                val kmStr = String.format(Locale.US, "≈ %.0f", pv.km_total)
                canvas.drawText(kmStr, vxKm + 4f, textBaselineRow, paintRowText)

                currentY += rowHeight
            }
            currentY += 15f
        }

        // Orders Table
        val indexedRows = ordenes.withIndex().toList()
        var rowIndex = 0

        if (currentY + headerHeight + rowHeight > pageHeight - 50f) {
            document.finishPage(page)
            page = document.startPage(pageInfo)
            canvas = page.canvas
            currentY = 50f
        }

        canvas.drawRect(marginLeft, currentY, pageWidth - marginRight, currentY + headerHeight, paintTableHeader)
        var textBaselineHeader = currentY + 15f
        canvas.drawText("#", xNo + 4f, textBaselineHeader, paintHeaderText)
        canvas.drawText("Fecha", xFecha + 4f, textBaselineHeader, paintHeaderText)
        canvas.drawText("Destino", xDestino + 4f, textBaselineHeader, paintHeaderText)
        canvas.drawText("Comb.", xComb + 4f, textBaselineHeader, paintHeaderText)
        canvas.drawText("Galones", xGalones + 4f, textBaselineHeader, paintHeaderText)
        canvas.drawText("Monto (L)", xMonto + 4f, textBaselineHeader, paintHeaderText)
        currentY += headerHeight

        while (rowIndex < indexedRows.size) {
            if (currentY + rowHeight > pageHeight - 50f) {
                document.finishPage(page)
                page = document.startPage(pageInfo)
                canvas = page.canvas
                currentY = 50f

                canvas.drawRect(marginLeft, currentY, pageWidth - marginRight, currentY + headerHeight, paintTableHeader)
                textBaselineHeader = currentY + 15f
                canvas.drawText("#", xNo + 4f, textBaselineHeader, paintHeaderText)
                canvas.drawText("Fecha", xFecha + 4f, textBaselineHeader, paintHeaderText)
                canvas.drawText("Destino", xDestino + 4f, textBaselineHeader, paintHeaderText)
                canvas.drawText("Comb.", xComb + 4f, textBaselineHeader, paintHeaderText)
                canvas.drawText("Galones", xGalones + 4f, textBaselineHeader, paintHeaderText)
                canvas.drawText("Monto (L)", xMonto + 4f, textBaselineHeader, paintHeaderText)
                currentY += headerHeight
            }

            val (originalIndex, orden) = indexedRows[rowIndex]
            canvas.drawLine(marginLeft, currentY, pageWidth - marginRight, currentY, paintRowLine)
            val textBaselineRow = currentY + 13f

            canvas.drawText((originalIndex + 1).toString(), xNo + 4f, textBaselineRow, paintRowText)
            canvas.drawText(formatearFechaIsoShort(orden.fecha_completada), xFecha + 4f, textBaselineRow, paintRowText)

            val destinoEllipsized = TextUtils.ellipsize(
                orden.destino,
                paintRowText,
                colDestinoW - 8f,
                TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(destinoEllipsized, xDestino + 4f, textBaselineRow, paintRowText)

            val combEllipsized = TextUtils.ellipsize(
                orden.tipo_combustible,
                paintRowText,
                colCombW - 8f,
                TextUtils.TruncateAt.END
            ).toString()
            canvas.drawText(combEllipsized, xComb + 4f, textBaselineRow, paintRowText)

            val galText = orden.galones_estimados?.let { String.format(Locale.US, "≈ %.2f", it) } ?: "N/D"
            canvas.drawText(galText, xGalones + 4f, textBaselineRow, paintRowText)

            val montoStr = String.format(Locale.US, "%,.2f", orden.monto)
            val montoWidth = paintRowText.measureText(montoStr)
            canvas.drawText(montoStr, (xMonto + colMontoW - 4f) - montoWidth, textBaselineRow, paintRowText)

            currentY += rowHeight
            rowIndex++
        }

        if (currentY + 25f > pageHeight - 50f) {
            document.finishPage(page)
            page = document.startPage(pageInfo)
            canvas = page.canvas
            currentY = 50f
        }
        canvas.drawLine(marginLeft, currentY, pageWidth - marginRight, currentY, paintRowLine)
        currentY += 4f
        val textBaselineTotal = currentY + 11f
        canvas.drawText("Total general", xNo + 4f, textBaselineTotal, paintTotalText)

        val totalMontoGeneral = ordenes.sumOf { it.monto }
        val totalGeneralStr = String.format(Locale.US, "%,.2f", totalMontoGeneral)
        val totalWidth = paintTotalText.measureText(totalGeneralStr)
        canvas.drawText(totalGeneralStr, (xMonto + colMontoW - 4f) - totalWidth, textBaselineTotal, paintTotalText)

        canvas.drawText("Galones y km estimados con el precio oficial vigente al asignar cada orden.", pageWidth / 2f, pageHeight - 25f, paintFooterNote)

        document.finishPage(page)

        val dir = File(context.cacheDir, "reportes")
        if (!dir.exists()) dir.mkdirs()

        val entidadLimpia = entidadIdOrNombre.replace(Regex("[^A-Za-z0-9]"), "_")
        val fileName = "reporte_${tipo}_${entidadLimpia}_${System.currentTimeMillis()}.pdf"
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
