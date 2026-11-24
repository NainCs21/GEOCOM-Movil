package com.example.apimovil.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.example.apimovil.data.ApiClient
import com.example.apimovil.data.models.Orden
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

data class EvidenciaImagen(
    val tipo: String,
    val destino: String,
    val bitmap: Bitmap
)

suspend fun generarFacturaPdf(
    context: Context,
    orden: Orden
): File? = withContext(Dispatchers.IO) {
    val listaImagenes = mutableListOf<EvidenciaImagen>()
    orden.detalles?.forEach { detalle ->
        val destinoLabel = if (detalle.tipo_destino == "vehiculo") {
            val v = detalle.vehiculo
            if (v != null) "${v.marca} ${v.modelo} (${v.placa})" else "Vehículo"
        } else {
            detalle.descripcion_mgp ?: "MGP"
        }
        detalle.evidencias.forEach { ev ->
            if (!ev.url.isNullOrBlank()) {
                val bitmap = descargarBitmap(normalizarUrlApi(ev.url))
                if (bitmap != null) {
                    listaImagenes.add(EvidenciaImagen(ev.tipo, destinoLabel, bitmap))
                }
            }
        }
    }

    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4

    val paintTitle = Paint().apply {
        color = Color.parseColor("#24212D")
        textSize = 15f
        isFakeBoldText = true
    }
    val paintSubtitle = Paint().apply {
        color = Color.parseColor("#666270")
        textSize = 10f
    }
    val paintHeader = Paint().apply {
        color = Color.parseColor("#24212D")
        textSize = 10f
    }
    val paintSectionTitle = Paint().apply {
        color = Color.parseColor("#6042A6")
        textSize = 12f
        isFakeBoldText = true
    }
    val paintTableHeader = Paint().apply { color = Color.parseColor("#6042A6") }
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
        color = Color.parseColor("#6042A6")
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

    try {
        // PAGINA 1
        val page1 = document.startPage(pageInfo)
        val canvas1 = page1.canvas
        var currentY = 50f

        canvas1.drawText("GEOCOM - Sistema de Órdenes de Combustible", marginLeft, currentY, paintHeader)
        currentY += 18f

        canvas1.drawText("FACTURA / COMPROBANTE DE ORDEN #${orden.numero_orden}", marginLeft, currentY, paintTitle)
        currentY += 18f

        val fechaGen = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "HN")).format(Date())
        canvas1.drawText("Generado el $fechaGen - Estado: ${orden.estado.uppercase()}", marginLeft, currentY, paintSubtitle)
        currentY += 22f

        canvas1.drawRect(marginLeft, currentY, pageWidth - marginRight, currentY + 70f, Paint().apply {
            color = Color.parseColor("#F5F3F8")
        })

        var infoY = currentY + 16f
        val conductorNombre = orden.conductor?.usuario?.name ?: "N/D"
        val gasolineraNombre = orden.gasolinera?.nombre ?: "N/D"
        val gasolineraDir = orden.gasolinera?.direccion.orEmpty()

        canvas1.drawText("Conductor: $conductorNombre", marginLeft + 12f, infoY, paintRowText)
        canvas1.drawText("Gasolinera: $gasolineraNombre", marginLeft + 260f, infoY, paintRowText)
        infoY += 16f

        canvas1.drawText("Dirección: $gasolineraDir", marginLeft + 260f, infoY, paintSubtitle)

        val fAsig = orden.fecha_asignacion?.let { formatoLegible(it) } ?: "N/D"
        val fComp = orden.fecha_completada?.let { formatoLegible(it) } ?: "N/D"
        canvas1.drawText("Asignada: $fAsig", marginLeft + 12f, infoY, paintSubtitle)
        infoY += 16f

        canvas1.drawText("Completada: $fComp", marginLeft + 12f, infoY, paintSubtitle)
        currentY += 85f

        canvas1.drawText("DESGLOSE DE COMBUSTIBLE", marginLeft, currentY, paintSectionTitle)
        currentY += 14f

        val colNoW = 25f
        val colDestinoW = 145f
        val colCombW = 85f
        val colLgalW = 60f
        val colGalonesW = 65f
        val colMontoW = contentWidth - colNoW - colDestinoW - colCombW - colLgalW - colGalonesW // 135f

        val xNo = marginLeft
        val xDestino = xNo + colNoW
        val xComb = xDestino + colDestinoW
        val xLgal = xComb + colCombW
        val xGalones = xLgal + colLgalW
        val xMonto = xGalones + colGalonesW

        val headerH = 22f
        canvas1.drawRect(marginLeft, currentY, pageWidth - marginRight, currentY + headerH, paintTableHeader)
        val textBaseH = currentY + 15f
        canvas1.drawText("#", xNo + 4f, textBaseH, paintHeaderText)
        canvas1.drawText("Destino / Vehículo", xDestino + 4f, textBaseH, paintHeaderText)
        canvas1.drawText("Combustible", xComb + 4f, textBaseH, paintHeaderText)
        canvas1.drawText("L/gal", xLgal + 4f, textBaseH, paintHeaderText)
        canvas1.drawText("Galones", xGalones + 4f, textBaseH, paintHeaderText)
        canvas1.drawText("Monto (L)", xMonto + 4f, textBaseH, paintHeaderText)
        currentY += headerH

        val detalles = orden.detalles.orEmpty()
        var itemNum = 1
        for (det in detalles) {
            canvas1.drawLine(marginLeft, currentY, pageWidth - marginRight, currentY, paintRowLine)
            val textBaseR = currentY + 15f

            canvas1.drawText(itemNum.toString(), xNo + 4f, textBaseR, paintRowText)

            val destText = if (det.tipo_destino == "vehiculo") {
                val v = det.vehiculo
                if (v != null) "${v.marca} ${v.modelo} (${v.placa})" else "Vehículo"
            } else {
                det.descripcion_mgp ?: "MGP"
            }

            val destEllipsized = TextUtils.ellipsize(destText, paintRowText, colDestinoW - 8f, TextUtils.TruncateAt.END).toString()
            canvas1.drawText(destEllipsized, xDestino + 4f, textBaseR, paintRowText)

            val combNombre = when (det.combustible_precio?.lowercase()?.trim()) {
                "diesel", "diésel" -> "Diésel"
                "regular" -> "Regular"
                "super", "súper" -> "Súper"
                null, "" -> det.tipo_combustible.replaceFirstChar { it.uppercase() }
                else -> det.combustible_precio.replaceFirstChar { it.uppercase() }
            }
            val combEllipsized = TextUtils.ellipsize(combNombre, paintRowText, colCombW - 8f, TextUtils.TruncateAt.END).toString()
            canvas1.drawText(combEllipsized, xComb + 4f, textBaseR, paintRowText)

            val lGalText = if (det.precio_galon != null) String.format(Locale.US, "%,.2f", det.precio_galon) else "N/D"
            canvas1.drawText(lGalText, xLgal + 4f, textBaseR, paintRowText)

            val galText = if (det.galones_estimados != null) String.format(Locale.US, "≈ %.2f", det.galones_estimados) else "N/D"
            canvas1.drawText(galText, xGalones + 4f, textBaseR, paintRowText)

            val montoFmt = String.format(Locale.US, "%,.2f", det.monto)
            val montoWidth = paintRowText.measureText(montoFmt)
            canvas1.drawText(montoFmt, (xMonto + colMontoW - 4f) - montoWidth, textBaseR, paintRowText)

            currentY += 22f
            itemNum++
        }

        canvas1.drawLine(marginLeft, currentY, pageWidth - marginRight, currentY, paintRowLine)
        currentY += 16f
        canvas1.drawText("TOTAL GENERAL", marginLeft + 4f, currentY, paintTotalText)
        val totalMonto = detalles.sumOf { it.monto }
        val totalFmt = String.format(Locale.US, "L %,.2f", totalMonto)
        val totalWidth = paintTotalText.measureText(totalFmt)
        canvas1.drawText(totalFmt, (pageWidth - marginRight - 4f) - totalWidth, currentY, paintTotalText)

        // Linea de estimacion
        val totalGalonesEstimados = detalles.mapNotNull { it.galones_estimados }.sum()
        val totalKmEstimados = detalles.filter { it.tipo_destino == "vehiculo" || it.vehiculo_id != null }.mapNotNull { it.km_estimados }.sum()
        val tieneEstimaciones = detalles.any { it.precio_galon != null || it.galones_estimados != null }

        if (tieneEstimaciones) {
            currentY += 18f
            val paintEstimacionLine = Paint().apply {
                color = Color.parseColor("#6042A6")
                textSize = 9.5f
                isFakeBoldText = true
            }
            val estText = "Combustible estimado: ≈ ${String.format(Locale.US, "%.2f", totalGalonesEstimados)} galones · ≈ ${String.format(Locale.US, "%.0f", totalKmEstimados)} km recorribles"
            canvas1.drawText(estText, marginLeft + 4f, currentY, paintEstimacionLine)

            val vigenteDesdeRaw = detalles.firstOrNull { !it.precio_vigente_desde.isNullOrBlank() }?.precio_vigente_desde
            val fechaVigenciaFmt = if (!vigenteDesdeRaw.isNullOrBlank()) formatearFechaIsoShort(vigenteDesdeRaw) else "esta semana"

            currentY += 14f
            val paintNote = TextPaint().apply {
                color = Color.parseColor("#666270")
                textSize = 8f
            }
            val notaText = "Estimación con el precio oficial de la Secretaría de Energía vigente desde $fechaVigenciaFmt (San Pedro Sula). Valores aproximados."
            val notaEllipsized = TextUtils.ellipsize(notaText, paintNote, contentWidth - 8f, TextUtils.TruncateAt.END).toString()
            canvas1.drawText(notaEllipsized, marginLeft + 4f, currentY, paintNote)
        }

        val totalPaginasEstimadas = if (listaImagenes.isEmpty()) 1 else 1 + ((listaImagenes.size + 1) / 2)
        canvas1.drawText("Página 1 de $totalPaginasEstimadas", pageWidth / 2f, pageHeight - 25f, paintFooter)

        document.finishPage(page1)

        if (listaImagenes.isNotEmpty()) {
            val chunked = listaImagenes.chunked(2)
            for ((imgPageIndex, chunk) in chunked.withIndex()) {
                val pageNum = imgPageIndex + 2
                val imgPage = document.startPage(pageInfo)
                val canvasImg = imgPage.canvas
                var yImg = 50f

                canvasImg.drawText("GEOCOM - Orden #${orden.numero_orden}", marginLeft, yImg, paintHeader)
                yImg += 18f

                canvasImg.drawText("EVIDENCIAS FOTOGRÁFICAS", marginLeft, yImg, paintSectionTitle)
                yImg += 22f

                for (evImg in chunk) {
                    canvasImg.drawText("Foto: ${evImg.tipo.uppercase()} - ${evImg.destino}", marginLeft, yImg, paintTitle)
                    yImg += 14f

                    val maxW = contentWidth
                    val maxH = 260f
                    val rect = RectF(marginLeft, yImg, marginLeft + maxW, yImg + maxH)

                    canvasImg.drawRoundRect(rect, 8f, 8f, Paint().apply {
                        color = Color.parseColor("#F5F3F8")
                    })

                    val bmp = evImg.bitmap
                    val bmpAspect = bmp.width.toFloat() / bmp.height.toFloat()
                    var drawW = maxW
                    var drawH = drawW / bmpAspect
                    if (drawH > maxH) {
                        drawH = maxH
                        drawW = drawH * bmpAspect
                    }

                    val left = marginLeft + (maxW - drawW) / 2f
                    val top = yImg + (maxH - drawH) / 2f
                    val destRect = RectF(left, top, left + drawW, top + drawH)

                    canvasImg.drawBitmap(bmp, null, destRect, null)
                    canvasImg.drawRoundRect(rect, 8f, 8f, Paint().apply {
                        color = Color.parseColor("#E5E0EB")
                        style = Paint.Style.STROKE
                        strokeWidth = 1f
                    })

                    yImg += maxH + 30f
                }

                canvasImg.drawText("Página $pageNum de $totalPaginasEstimadas", pageWidth / 2f, pageHeight - 25f, paintFooter)
                document.finishPage(imgPage)
            }
        }

        val dir = File(context.cacheDir, "facturas")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "factura_orden_${orden.numero_orden}.pdf")
        val fos = FileOutputStream(file)
        document.writeTo(fos)
        fos.close()
        document.close()

        // Reciclar bitmaps para liberar memoria
        listaImagenes.forEach { it.bitmap.recycle() }

        file
    } catch (_: Exception) {
        try {
            listaImagenes.forEach { it.bitmap.recycle() }
            document.close()
        } catch (_: Exception) {}
        null
    }
}

private suspend fun descargarBitmap(urlStr: String?): Bitmap? = withContext(Dispatchers.IO) {
    if (urlStr.isNullOrBlank()) return@withContext null
    try {
        val request = Request.Builder().url(urlStr).build()
        ApiClient.client.newCall(request).execute().use { response ->
            val body = response.body
            if (response.isSuccessful && body != null) {
                val bytes = body.bytes()
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                val maxDimension = 1200
                var sampleSize = 1
                val width = options.outWidth
                val height = options.outHeight
                if (width > maxDimension || height > maxDimension) {
                    val halfWidth = width / 2
                    val halfHeight = height / 2
                    while ((halfWidth / sampleSize) >= maxDimension || (halfHeight / sampleSize) >= maxDimension) {
                        sampleSize *= 2
                    }
                }

                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
            } else {
                null
            }
        }
    } catch (_: Exception) {
        null
    }
}
