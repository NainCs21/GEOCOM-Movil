package com.example.apimovil.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.max

internal const val PAGE_W = 595
internal const val PAGE_H = 842
internal const val MARGIN = 40f
internal const val CONTENT_W = 515f
internal const val PIE_ALTO = 48f

internal val COLOR_MORADO = 0xFF6042A6.toInt()
internal val COLOR_TEXTO = 0xFF24212D.toInt()
internal val COLOR_GRIS = 0xFF666270.toInt()
internal val COLOR_LINEA = 0xFFE5E0EB.toInt()
internal val COLOR_FONDO = 0xFFF5F3F8.toInt()
internal val COLOR_ZEBRA = 0xFFFAF9FC.toInt()

internal fun pintura(tam: Float, color: Int, negrita: Boolean = false): TextPaint {
    return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = tam
        this.color = color
        typeface = if (negrita) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }
}

internal fun relleno(color: Int): Paint {
    return Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
}

internal fun monto(v: Double): String {
    return String.format(Locale.US, "%,.2f", v)
}

internal fun texto(
    contenido: String,
    paint: TextPaint,
    ancho: Float,
    alineacion: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
): StaticLayout {
    return StaticLayout.Builder.obtain(contenido, 0, contenido.length, paint, ancho.toInt())
        .setAlignment(alineacion)
        .setIncludePad(false)
        .build()
}

internal fun ajustar(paint: TextPaint, contenido: String, ancho: Float, minimo: Float = 6f): TextPaint {
    val tp = TextPaint(paint)
    while (tp.textSize > minimo && tp.measureText(contenido) > ancho) {
        tp.textSize = (tp.textSize - 0.5f).coerceAtLeast(minimo)
    }
    return tp
}

internal class LienzoPdf(
    private val titulo: String,
    private val notaPie: String,
    private val totalPaginas: Int?
) {
    val document = PdfDocument()
    var numero = 1
        private set
    var canvas: Canvas
        private set
    var y = MARGIN

    private var pageInfo: PdfDocument.PageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, numero).create()
    private var page: PdfDocument.Page = document.startPage(pageInfo)

    init {
        canvas = page.canvas
        dibujarBandaSuperior()
    }

    private fun dibujarBandaSuperior() {
        val p = relleno(COLOR_MORADO)
        canvas.drawRect(0f, 0f, PAGE_W.toFloat(), 6f, p)
        if (numero > 1) {
            val pGris = pintura(8.5f, COLOR_GRIS)
            val l = texto("GEOCOM · $titulo", pGris, CONTENT_W)
            canvas.save()
            canvas.translate(MARGIN, MARGIN - 12f)
            l.draw(canvas)
            canvas.restore()

            val pLine = Paint().apply {
                color = COLOR_LINEA
                strokeWidth = 1f
            }
            canvas.drawLine(MARGIN, MARGIN + 4f, PAGE_W - MARGIN, MARGIN + 4f, pLine)
            y = MARGIN + 16f
        } else {
            y = MARGIN
        }
    }

    fun nuevaPagina() {
        dibujarPie()
        document.finishPage(page)
        numero++
        pageInfo = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, numero).create()
        page = document.startPage(pageInfo)
        canvas = page.canvas
        dibujarBandaSuperior()
    }

    fun asegurar(alto: Float): Boolean {
        if (y + alto > PAGE_H - PIE_ALTO && y > MARGIN + 20f) {
            nuevaPagina()
            return true
        }
        return false
    }

    fun dibujar(layout: StaticLayout, x: Float, currentY: Float) {
        canvas.save()
        canvas.translate(x, currentY)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun dibujarPie() {
        val pLine = Paint().apply {
            color = COLOR_LINEA
            strokeWidth = 1f
        }
        val py = PAGE_H - 38f
        canvas.drawLine(MARGIN, py, PAGE_W - MARGIN, py, pLine)

        val pPie = pintura(7.5f, COLOR_GRIS)
        val lNota = texto(notaPie, pPie, CONTENT_W * 0.65f)
        canvas.save()
        canvas.translate(MARGIN, py + 6f)
        lNota.draw(canvas)
        canvas.restore()

        val numTotal = totalPaginas ?: numero
        val pagTexto = "Página $numero de $numTotal"
        val pNum = pintura(7.5f, COLOR_GRIS)
        val lNum = texto(pagTexto, pNum, CONTENT_W * 0.35f, Layout.Alignment.ALIGN_OPPOSITE)
        canvas.save()
        canvas.translate(PAGE_W - MARGIN - (CONTENT_W * 0.35f), py + 6f)
        lNum.draw(canvas)
        canvas.restore()
    }

    fun cerrar(): Int {
        dibujarPie()
        document.finishPage(page)
        return numero
    }
}

internal fun renderizar(titulo: String, notaPie: String, contenido: (LienzoPdf) -> Unit): PdfDocument {
    val lienzoTemp = LienzoPdf(titulo, notaPie, null)
    contenido(lienzoTemp)
    val total = lienzoTemp.cerrar()
    lienzoTemp.document.close()

    val lienzoFinal = LienzoPdf(titulo, notaPie, total)
    contenido(lienzoFinal)
    lienzoFinal.cerrar()
    return lienzoFinal.document
}

internal fun guardarPdf(context: Context, document: PdfDocument, carpeta: String, nombre: String): File {
    val dir = File(context.cacheDir, carpeta).apply { mkdirs() }
    val nombreLimpio = nombreArchivoSeguro(nombre)
    val nombreConExt = if (nombreLimpio.endsWith(".pdf", ignoreCase = true)) nombreLimpio else "$nombreLimpio.pdf"
    val file = File(dir, nombreConExt)

    val canonicalPath = file.canonicalPath
    val dirCanonicalPath = dir.canonicalPath
    if (!canonicalPath.startsWith(dirCanonicalPath)) {
        throw SecurityException("Ruta de archivo no válida.")
    }

    FileOutputStream(file).use { out ->
        document.writeTo(out)
    }
    document.close()
    return file
}

internal fun encabezado(l: LienzoPdf, titulo: String, lineas: List<String>) {
    val pMarca = pintura(13f, COLOR_MORADO, true)
    val lMarca = texto("GEOCOM", pMarca, CONTENT_W * 0.5f)
    l.canvas.save()
    l.canvas.translate(MARGIN, l.y)
    lMarca.draw(l.canvas)
    l.canvas.restore()

    val pSis = pintura(9f, COLOR_GRIS)
    val lSis = texto("Sistema de Órdenes de Combustible", pSis, CONTENT_W * 0.5f, Layout.Alignment.ALIGN_OPPOSITE)
    l.canvas.save()
    l.canvas.translate(MARGIN + CONTENT_W * 0.5f, l.y + 4f)
    lSis.draw(l.canvas)
    l.canvas.restore()

    l.y += 22f

    val pTit = pintura(17f, COLOR_MORADO, true)
    val lTit = texto(titulo, pTit, CONTENT_W)
    l.asegurar(lTit.height.toFloat() + 10f)
    l.dibujar(lTit, MARGIN, l.y)
    l.y += lTit.height + 6f

    val pSub = pintura(9.5f, COLOR_GRIS)
    for (linea in lineas) {
        if (linea.isNotBlank()) {
            val lSub = texto(linea, pSub, CONTENT_W)
            l.asegurar(lSub.height.toFloat() + 6f)
            l.dibujar(lSub, MARGIN, l.y)
            l.y += lSub.height + 4f
        }
    }

    l.y += 6f
    val pLine = Paint().apply {
        color = COLOR_MORADO
        strokeWidth = 1.5f
    }
    l.canvas.drawLine(MARGIN, l.y, PAGE_W - MARGIN, l.y, pLine)
    l.y += 14f
}

internal fun seccion(l: LienzoPdf, titulo: String) {
    val pBar = relleno(COLOR_MORADO)
    l.canvas.drawRect(MARGIN, l.y, MARGIN + 3f, l.y + 13f, pBar)

    val pTit = pintura(11f, COLOR_MORADO, true)
    val lTit = texto(titulo, pTit, CONTENT_W - 10f)
    l.canvas.save()
    l.canvas.translate(MARGIN + 8f, l.y - 1f)
    lTit.draw(l.canvas)
    l.canvas.restore()

    l.y += 22f
}

internal class ColumnaPdf(val titulo: String, val ancho: Float, val derecha: Boolean = false)

internal fun tabla(
    l: LienzoPdf,
    titulo: String,
    columnas: List<ColumnaPdf>,
    filas: List<List<String>>,
    totales: List<String>? = null,
    tam: Float = 8.5f
) {
    val pHead = pintura(tam, COLOR_MORADO, true)
    val pBody = pintura(tam, COLOR_TEXTO)
    val alturaEstFila = max(pHead.textSize, pBody.textSize) + 10f
    seccion(l, titulo)
    l.asegurar(alturaEstFila * 3f)

    val pintarEncabezadoTabla = {
        val pBg = relleno(COLOR_MORADO)
        l.canvas.drawRect(MARGIN, l.y, PAGE_W - MARGIN, l.y + alturaEstFila, pBg)

        var cx = MARGIN
        for (col in columnas) {
            val align = if (col.derecha) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
            val lC = texto(col.titulo, pintura(tam, 0xFFFFFFFF.toInt(), true), col.ancho - 10f, align)
            l.dibujar(lC, cx + 5f, l.y + 5f)
            cx += col.ancho
        }
        l.y += alturaEstFila
    }

    pintarEncabezadoTabla()

    val pLinea = Paint().apply {
        color = COLOR_LINEA
        strokeWidth = 0.8f
    }

    filas.forEachIndexed { index, fila ->
        var maxLineas = 1
        val layouts = mutableListOf<StaticLayout>()
        columnas.forEachIndexed { colIdx, col ->
            val textoCelda = fila.getOrNull(colIdx).orEmpty()
            val align = if (col.derecha) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
            val lC = texto(textoCelda, pBody, col.ancho - 10f, align)
            layouts.add(lC)
            maxLineas = max(maxLineas, lC.lineCount)
        }
        val alturaFila = maxLineas * pBody.textSize + 10f

        if (l.asegurar(alturaFila + 4f)) {
            pintarEncabezadoTabla()
        }

        if (index % 2 == 1) {
            val pZebra = relleno(COLOR_ZEBRA)
            l.canvas.drawRect(MARGIN, l.y, PAGE_W - MARGIN, l.y + alturaFila, pZebra)
        }

        var cx = MARGIN
        columnas.forEachIndexed { colIdx, col ->
            val lC = layouts[colIdx]
            l.dibujar(lC, cx + 5f, l.y + 5f)
            cx += col.ancho
        }

        l.canvas.drawLine(MARGIN, l.y + alturaFila, PAGE_W - MARGIN, l.y + alturaFila, pLinea)
        l.y += alturaFila
    }

    if (!totales.isNullOrEmpty()) {
        val pTot = pintura(tam, COLOR_TEXTO, true)
        var maxLineas = 1
        val layouts = mutableListOf<StaticLayout>()
        columnas.forEachIndexed { colIdx, col ->
            val textoCelda = totales.getOrNull(colIdx).orEmpty()
            val align = if (col.derecha) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
            val lC = texto(textoCelda, pTot, col.ancho - 10f, align)
            layouts.add(lC)
            maxLineas = max(maxLineas, lC.lineCount)
        }
        val alturaFila = maxLineas * pTot.textSize + 10f

        if (l.asegurar(alturaFila + 6f)) {
            pintarEncabezadoTabla()
        }

        val pFondo = relleno(COLOR_FONDO)
        l.canvas.drawRect(MARGIN, l.y, PAGE_W - MARGIN, l.y + alturaFila, pFondo)

        val pTop = Paint().apply {
            color = COLOR_MORADO
            strokeWidth = 1.5f
        }
        l.canvas.drawLine(MARGIN, l.y, PAGE_W - MARGIN, l.y, pTop)

        var cx = MARGIN
        columnas.forEachIndexed { colIdx, col ->
            val lC = layouts[colIdx]
            l.dibujar(lC, cx + 5f, l.y + 5f)
            cx += col.ancho
        }
        l.y += alturaFila + 4f
    }
}
