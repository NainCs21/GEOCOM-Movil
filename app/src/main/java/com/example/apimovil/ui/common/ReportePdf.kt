package com.example.apimovil.ui.common

import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import com.example.apimovil.data.models.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

data class FilaReportePdf(
    val entidad: String,
    val cantidadOrdenes: Int,
    val montoTotal: Double
)

data class BarraPdf(val etiqueta: String, val valor: Double, val color: Int = COLOR_MORADO)

private fun indicadores(l: LienzoPdf, resumen: ResumenReporte) {
    seccion(l, "Resumen del periodo")
    val boxH = 42f
    val gap = 8f
    val cols = 4
    val rows = 2
    val colW = (CONTENT_W - (cols - 1) * gap) / cols
    val totalH = rows * boxH + (rows - 1) * gap

    l.asegurar(totalH + 10f)

    val precPromStr = if (resumen.precio_promedio_galon != null) "L ${monto(resumen.precio_promedio_galon)}" else "N/D"
    val galStr = String.format(Locale.US, "≈ %,.1f", resumen.galones_total)

    val items = listOf(
        "Gasto total" to "L ${monto(resumen.monto_total)}",
        "Órdenes" to resumen.cantidad_ordenes.toString(),
        "Promedio por orden" to "L ${monto(resumen.promedio_orden)}",
        "Cumplimiento" to String.format(Locale.US, "%.1f%%", resumen.cumplimiento_pct),
        "Completadas tarde" to resumen.completadas_tarde.toString(),
        "Vencidas" to resumen.vencidas.toString(),
        "Galones (aprox.)" to galStr,
        "Precio prom. galón" to precPromStr
    )

    val pBg = relleno(COLOR_FONDO)
    val pLabel = pintura(7.5f, COLOR_GRIS)
    val pVal = pintura(13f, COLOR_MORADO, true)

    for ((i, par) in items.withIndex()) {
        val r = i / cols
        val c = i % cols
        val x = MARGIN + c * (colW + gap)
        val currentY = l.y + r * (boxH + gap)

        l.canvas.drawRoundRect(RectF(x, currentY, x + colW, currentY + boxH), 6f, 6f, pBg)

        val lLbl = texto(par.first, ajustar(pLabel, par.first, colW - 12f), colW - 12f)
        l.dibujar(lLbl, x + 6f, currentY + 6f)

        val lVal = texto(par.second, ajustar(pVal, par.second, colW - 12f), colW - 12f)
        l.dibujar(lVal, x + 6f, currentY + 20f)
    }

    l.y += totalH + 12f
}

private fun graficoVertical(l: LienzoPdf, titulo: String, barras: List<BarraPdf>) {
    if (barras.isEmpty()) return
    seccion(l, titulo)

    val chartH = 110f
    l.asegurar(chartH + 35f)

    val maxVal = max(barras.maxOfOrNull { it.valor } ?: 1.0, 1.0)
    val bottomY = l.y + chartH
    val topY = l.y + 15f

    val pLine = Paint().apply {
        color = COLOR_LINEA
        strokeWidth = 1f
    }
    l.canvas.drawLine(MARGIN, bottomY, PAGE_W - MARGIN, bottomY, pLine)

    val slotW = CONTENT_W / barras.size
    val barW = (slotW * 0.6f).coerceAtMost(30f)

    val pText = pintura(8.5f, COLOR_TEXTO)
    pText.textAlign = Paint.Align.CENTER

    barras.forEachIndexed { i, b ->
        val cx = MARGIN + slotW * i + slotW / 2f
        val barH = ((bottomY - topY) * (b.valor / maxVal)).toFloat()
        val barTop = bottomY - barH

        val pBar = relleno(b.color)
        l.canvas.drawRoundRect(RectF(cx - barW / 2f, barTop, cx + barW / 2f, bottomY), 3f, 3f, pBar)

        val valStr = if (b.valor >= 1000) String.format(Locale.US, "%.1fk", b.valor / 1000) else String.format(Locale.US, "%.0f", b.valor)
        val lVal = texto(valStr, pText, slotW)
        l.dibujar(lVal, cx - slotW / 2f, barTop - 12f)

        val etiqueta = texto(b.etiqueta, pText, slotW)
        l.dibujar(etiqueta, cx - slotW / 2f, bottomY + 4f)
    }
    pText.textAlign = Paint.Align.LEFT
    l.y = bottomY + 26f
}

private fun graficoHorizontal(l: LienzoPdf, titulo: String, barras: List<BarraPdf>) {
    if (barras.isEmpty()) return
    seccion(l, titulo)

    val pValText = pintura(9f, COLOR_TEXTO, true)
    val maxValStr = barras.maxOfOrNull { "L ${monto(it.valor)}" } ?: "L 0.00"
    val valW = pValText.measureText(maxValStr) + 10f

    val labelW = CONTENT_W * 0.34f
    val barMaxW = CONTENT_W - labelW - valW - 12f
    val rowH = 22f

    l.asegurar(barras.size * rowH + 20f)

    val maxVal = max(barras.maxOfOrNull { it.valor } ?: 1.0, 1.0)
    val pText = pintura(9f, COLOR_TEXTO)
    val pTrack = relleno(COLOR_FONDO)

    barras.forEach { b ->
        l.asegurar(rowH)
        val currentY = l.y

        val lLabel = texto(b.etiqueta, pText, labelW)
        l.dibujar(lLabel, MARGIN, currentY)

        val trackX = MARGIN + labelW + 6f
        val trackW = barMaxW
        l.canvas.drawRoundRect(RectF(trackX, currentY + 3f, trackX + trackW, currentY + rowH - 5f), 3f, 3f, pTrack)

        val barW = (trackW * (b.valor / maxVal)).toFloat().coerceAtLeast(3f)
        val pBar = relleno(b.color)
        l.canvas.drawRoundRect(RectF(trackX, currentY + 3f, trackX + barW, currentY + rowH - 5f), 3f, 3f, pBar)

        val valStr = "L ${monto(b.valor)}"
        val lVal = texto(valStr, pValText, valW, Layout.Alignment.ALIGN_OPPOSITE)
        l.dibujar(lVal, trackX + trackW + 6f, currentY + 3f)

        l.y += rowH
    }
    l.y += 10f
}

private const val NOTA_PIE = "Galones y km estimados con el precio oficial vigente al asignar cada orden."

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
    try {
        val fechaGen = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "HN")).format(Date())
        val tituloReal = if (!nombreEntidadFiltro.isNullOrBlank()) "Reporte de consumo: $nombreEntidadFiltro" else tituloReporte
        val periodoStr = if (textoPeriodo.isBlank()) "Órdenes completadas, todo el periodo" else "Órdenes completadas del $textoPeriodo"
        val genStr = "Generado el $fechaGen por ${nombreGenerador ?: "Administrador"}"

        val doc = renderizar(tituloReal, NOTA_PIE) { l ->
            encabezado(l, tituloReal, listOf(periodoStr, genStr))

            if (resumen != null) {
                indicadores(l, resumen.resumen)
                graficoVertical(l, "Gasto por mes", resumen.por_mes.map { BarraPdf(formatearMes(it.mes), it.monto_total) })

                val rankingList = if (esConductor) {
                    val top = resumen.top_conductores.take(8)
                    val colores = PaletaGrafica.asignar(top.map { it.id })
                    top.mapIndexed { i, c -> BarraPdf(c.nombre, c.monto_total, colores[i]) }
                } else {
                    resumen.top_gasolineras.take(8).map { BarraPdf(it.nombre, it.monto_total, COLOR_MORADO) }
                }
                val rankingTitle = if (esConductor) "Top conductores por gasto" else "Top gasolineras por gasto"
                graficoHorizontal(l, rankingTitle, rankingList)
            }

            val columnas = listOf(
                ColumnaPdf("#", 30f),
                ColumnaPdf(nombreColumnaEntidad, CONTENT_W - 30f - 70f - 105f),
                ColumnaPdf("Órdenes", 70f, true),
                ColumnaPdf("Monto (L)", 105f, true)
            )

            val filasTabla = filas.mapIndexed { idx, row ->
                listOf(
                    (idx + 1).toString(),
                    row.entidad,
                    row.cantidadOrdenes.toString(),
                    monto(row.montoTotal)
                )
            }

            val totales = listOf("Total general", "", totalOrdenes.toString(), monto(montoTotalGeneral))
            tabla(l, if (esConductor) "Detalle por conductor" else "Detalle por gasolinera", columnas, filasTabla, totales)
        }

        val tipoPrefijo = if (esConductor) "reporte_conductor" else "reporte_gasolinera"
        val periodoLimpio = if (desde.isNullOrBlank() || hasta.isNullOrBlank()) "todo_el_periodo" else "${desde}_a_$hasta"
        guardarPdf(context, doc, "reportes", "${tipoPrefijo}_$periodoLimpio.pdf")
    } catch (_: Exception) {
        null
    }
}

suspend fun generarReporteDetallePdf(
    context: Context,
    tipo: String,
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
    try {
        val fechaGen = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es", "HN")).format(Date())
        val tituloReal = "Reporte de consumo: $nombre"
        val periodoStr = if (textoPeriodo.isBlank()) "Órdenes completadas, todo el periodo" else "Órdenes completadas del $textoPeriodo"
        val genStr = "Generado el $fechaGen por ${nombreGenerador ?: "Administrador"}"
        val lineasEncabezado = mutableListOf<String>()
        if (!subtitulo.isNullOrBlank()) lineasEncabezado.add(subtitulo)
        lineasEncabezado.add(periodoStr)
        lineasEncabezado.add(genStr)

        val doc = renderizar(tituloReal, NOTA_PIE) { l ->
            encabezado(l, tituloReal, lineasEncabezado)
            indicadores(l, resumen)
            graficoVertical(l, "Gasto por mes", porMes.map { BarraPdf(formatearMes(it.mes), it.monto_total) })
            graficoHorizontal(l, "Gasto por combustible", porCombustible.map {
                val tipoComb = if (it.tipo_combustible.lowercase().contains("gas")) "Gasolina" else "Diésel"
                BarraPdf(tipoComb, it.monto_total)
            })

            val rankingTitle = if (tipo == "conductor") "Gasolineras donde cargó" else "Conductores que la usan"
            val rankingBarras = if (tipo == "conductor") {
                ranking.take(8).map { BarraPdf(it.nombre, it.monto_total, COLOR_MORADO) }
            } else {
                val top = ranking.take(8)
                val colores = PaletaGrafica.asignar(top.map { it.id })
                top.mapIndexed { i, r -> BarraPdf(r.nombre, r.monto_total, colores[i]) }
            }
            graficoHorizontal(l, rankingTitle, rankingBarras)

            if (tipo == "conductor" && porVehiculo.isNotEmpty()) {
                val columnasVeh = listOf(
                    ColumnaPdf("Placa", 65f),
                    ColumnaPdf("Descripción", CONTENT_W - 65f - 85f - 80f - 90f),
                    ColumnaPdf("Gasto (L)", 85f, true),
                    ColumnaPdf("Galones", 80f, true),
                    ColumnaPdf("Km aprox.", 90f, true)
                )
                val filasVeh = porVehiculo.map { v ->
                    listOf(
                        v.placa,
                        v.descripcion,
                        monto(v.monto_total),
                        String.format(Locale.US, "≈ %.1f", v.galones_total),
                        String.format(Locale.US, "≈ %.0f", v.km_total)
                    )
                }
                tabla(l, "Vehículos", columnasVeh, filasVeh)
            }

            val columnasOrd = listOf(
                ColumnaPdf("#", 28f),
                ColumnaPdf("Fecha", 62f),
                ColumnaPdf("Destino", CONTENT_W - 28f - 62f - 62f - 70f - 85f),
                ColumnaPdf("Comb.", 62f),
                ColumnaPdf("Galones", 70f, true),
                ColumnaPdf("Monto (L)", 85f, true)
            )

            val filasOrd = ordenes.mapIndexed { idx, o ->
                listOf(
                    (idx + 1).toString(),
                    formatearFechaIsoShort(o.fecha_completada),
                    o.destino,
                    o.tipo_combustible,
                    o.galones_estimados?.let { String.format(Locale.US, "≈ %.2f", it) } ?: "N/D",
                    monto(o.monto)
                )
            }

            val totalMontoOrdenes = ordenes.sumOf { it.monto }
            val totalesOrd = listOf("Total general", "", "", "", "", monto(totalMontoOrdenes))
            tabla(l, "Órdenes del periodo", columnasOrd, filasOrd, totalesOrd)
        }

        val entidadLimpia = entidadIdOrNombre.replace(Regex("[^A-Za-z0-9]"), "_")
        val fileName = "reporte_${tipo}_${entidadLimpia}_${System.currentTimeMillis()}.pdf"
        guardarPdf(context, doc, "reportes", fileName)
    } catch (_: Exception) {
        null
    }
}
