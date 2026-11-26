package com.example.apimovil.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import com.example.apimovil.data.ApiClient
import com.example.apimovil.data.models.Orden
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import kotlin.math.max

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
    try {
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

        val fechaGen = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "HN")).format(Date())
        val tituloReal = "Orden #${orden.numero_orden}"
        val notaPie = "Comprobante generado por GEOCOM"

        val document = renderizar(tituloReal, notaPie) { l ->
            encabezado(l, "Factura / Comprobante de orden #${orden.numero_orden}", listOf("Generado el $fechaGen · Estado: ${orden.estado.uppercase()}"))

            // Datos de la orden (Card con fondo FONDO)
            seccion(l, "Datos de la orden")
            val boxWidth = CONTENT_W
            val colW = (boxWidth - 24f) / 2f
            val pLbl = pintura(8f, COLOR_GRIS)
            val pVal = pintura(10f, COLOR_TEXTO, true)

            val conductorNombre = orden.conductor?.usuario?.name ?: "N/D"
            val fAsig = orden.fecha_asignacion?.let { formatoLegible(it) } ?: "N/D"
            val fComp = orden.fecha_completada?.let { formatoLegible(it) } ?: "N/D"

            val gasolineraNombre = orden.gasolinera?.nombre ?: "N/D"
            val gasolineraDir = orden.gasolinera?.direccion.orEmpty()

            val leftItems = listOf("Conductor" to conductorNombre, "Asignada" to fAsig, "Completada" to fComp)
            val rightItems = mutableListOf<Pair<String, String>>()
            rightItems.add("Gasolinera" to gasolineraNombre)
            if (gasolineraDir.isNotBlank()) {
                rightItems.add("Dirección" to gasolineraDir)
            }

            var leftH = 12f
            leftItems.forEach { (lbl, v) ->
                val lLbl = texto(lbl, pLbl, colW)
                val lVal = texto(v, pVal, colW)
                leftH += lLbl.height + lVal.height + 6f
            }
            leftH += 6f

            var rightH = 12f
            rightItems.forEach { (lbl, v) ->
                val lLbl = texto(lbl, pLbl, colW)
                val lVal = texto(v, pVal, colW)
                rightH += lLbl.height + lVal.height + 6f
            }
            rightH += 6f

            val boxH = max(leftH, rightH)
            l.asegurar(boxH + 15f)

            val boxY = l.y
            l.canvas.drawRoundRect(RectF(MARGIN, boxY, MARGIN + boxWidth, boxY + boxH), 8f, 8f, relleno(COLOR_FONDO))

            var currentLeftY = boxY + 12f
            leftItems.forEach { (lbl, v) ->
                val lLbl = texto(lbl, pLbl, colW)
                l.dibujar(lLbl, MARGIN + 12f, currentLeftY)
                currentLeftY += lLbl.height + 2f
                val lVal = texto(v, pVal, colW)
                l.dibujar(lVal, MARGIN + 12f, currentLeftY)
                currentLeftY += lVal.height + 4f
            }

            var currentRightY = boxY + 12f
            val rightX = MARGIN + 12f + colW + 12f
            rightItems.forEach { (lbl, v) ->
                val lLbl = texto(lbl, pLbl, colW)
                l.dibujar(lLbl, rightX, currentRightY)
                currentRightY += lLbl.height + 2f
                val lVal = texto(v, pVal, colW)
                l.dibujar(lVal, rightX, currentRightY)
                currentRightY += lVal.height + 4f
            }

            l.y = boxY + boxH + 14f

            // Tabla Desglose de combustible
            val detalles = orden.detalles.orEmpty()
            val colNoW = 28f
            val colCombW = 80f
            val colLgalW = 55f
            val colGalonesW = 65f
            val colMontoW = 85f
            val colDestinoW = CONTENT_W - colNoW - colCombW - colLgalW - colGalonesW - colMontoW

            val columnas = listOf(
                ColumnaPdf("#", colNoW),
                ColumnaPdf("Destino / Vehículo", colDestinoW),
                ColumnaPdf("Combustible", colCombW),
                ColumnaPdf("L/gal", colLgalW, true),
                ColumnaPdf("Galones", colGalonesW, true),
                ColumnaPdf("Monto (L)", colMontoW, true)
            )

            val filas = mutableListOf<List<String>>()
            detalles.forEachIndexed { idx, det ->
                val destText = if (det.tipo_destino == "vehiculo") {
                    val v = det.vehiculo
                    if (v != null) "${v.marca} ${v.modelo} (${v.placa})" else "Vehículo"
                } else {
                    det.descripcion_mgp ?: "MGP"
                }

                val combNombre = when (det.combustible_precio?.lowercase()?.trim()) {
                    "diesel", "diésel" -> "Diésel"
                    "regular" -> "Regular"
                    "super", "súper" -> "Súper"
                    null, "" -> det.tipo_combustible.replaceFirstChar { it.uppercase() }
                    else -> det.combustible_precio.replaceFirstChar { it.uppercase() }
                }

                val lGalText = if (det.precio_galon != null) String.format(Locale.US, "%,.2f", det.precio_galon) else "N/D"
                val galText = if (det.galones_estimados != null) String.format(Locale.US, "≈ %.2f", det.galones_estimados) else "N/D"

                filas.add(
                    listOf(
                        (idx + 1).toString(),
                        destText,
                        combNombre,
                        lGalText,
                        galText,
                        monto(det.monto)
                    )
                )
            }

            val totalMonto = detalles.sumOf { it.monto }
            val totales = listOf("TOTAL GENERAL", "", "", "", "", monto(totalMonto))
            tabla(l, "Desglose de combustible", columnas, filas, totales)

            // Estimaciones
            val totalGalonesEstimados = detalles.mapNotNull { it.galones_estimados }.sum()
            val totalKmEstimados = detalles.filter { it.tipo_destino == "vehiculo" || it.vehiculo_id != null }.mapNotNull { it.km_estimados }.sum()
            val tieneEstimaciones = detalles.any { it.precio_galon != null || it.galones_estimados != null }

            if (tieneEstimaciones) {
                l.asegurar(45f)
                l.y += 6f
                val pEst = pintura(9.5f, COLOR_MORADO, true)
                val estText = "Combustible estimado: ≈ ${String.format(Locale.US, "%.2f", totalGalonesEstimados)} galones · ≈ ${String.format(Locale.US, "%.0f", totalKmEstimados)} km recorribles"
                val lEst = texto(estText, pEst, CONTENT_W)
                l.dibujar(lEst, MARGIN, l.y)
                l.y += lEst.height + 4f

                val vigenteDesdeRaw = detalles.firstOrNull { !it.precio_vigente_desde.isNullOrBlank() }?.precio_vigente_desde
                val fechaVigenciaFmt = if (!vigenteDesdeRaw.isNullOrBlank()) formatearFechaIsoShort(vigenteDesdeRaw) else "esta semana"

                val pNote = pintura(8f, COLOR_GRIS)
                val notaText = "Estimación con el precio oficial de la Secretaría de Energía vigente desde $fechaVigenciaFmt (San Pedro Sula). Valores aproximados."
                val lNote = texto(notaText, pNote, CONTENT_W)
                l.asegurar(lNote.height.toFloat() + 10f)
                l.dibujar(lNote, MARGIN, l.y)
                l.y += lNote.height + 10f
            }

            // Evidencias fotográficas
            if (listaImagenes.isNotEmpty()) {
                seccion(l, "Evidencias fotográficas")
                for (evImg in listaImagenes) {
                    l.asegurar(35f + 260f)
                    val pCap = pintura(9.5f, COLOR_TEXTO, true)
                    val capText = "Foto: ${evImg.tipo.uppercase()} · ${evImg.destino}"
                    val lCap = texto(capText, pCap, CONTENT_W)
                    l.dibujar(lCap, MARGIN, l.y)
                    l.y += lCap.height + 6f

                    val maxW = CONTENT_W
                    val maxH = 260f
                    val currentY = l.y
                    l.canvas.drawRoundRect(RectF(MARGIN, currentY, MARGIN + maxW, currentY + maxH), 8f, 8f, relleno(COLOR_FONDO))

                    val bmp = evImg.bitmap
                    val bmpAspect = bmp.width.toFloat() / bmp.height.toFloat()
                    var drawW = maxW
                    var drawH = drawW / bmpAspect
                    if (drawH > maxH) {
                        drawH = maxH
                        drawW = drawH * bmpAspect
                    }

                    val left = MARGIN + (maxW - drawW) / 2f
                    val top = currentY + (maxH - drawH) / 2f
                    val destRect = RectF(left, top, left + drawW, top + drawH)
                    l.canvas.drawBitmap(bmp, null, destRect, null)

                    val pBorder = Paint().apply {
                        color = COLOR_LINEA
                        style = Paint.Style.STROKE
                        strokeWidth = 1f
                    }
                    l.canvas.drawRoundRect(RectF(MARGIN, currentY, MARGIN + maxW, currentY + maxH), 8f, 8f, pBorder)

                    l.y += maxH + 16f
                }
            }
        }

        val file = guardarPdf(context, document, "facturas", "factura_orden_${orden.numero_orden}.pdf")
        listaImagenes.forEach { it.bitmap.recycle() }
        file
    } catch (_: Exception) {
        try {
            listaImagenes.forEach { it.bitmap.recycle() }
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
