package com.example.apimovil.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.appcompat.R
import java.util.Locale
import kotlin.math.max

class BarChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    data class Barra(val etiqueta: String, val valor: Double)

    /** true: barras horizontales (ranking). false: barras verticales (serie por mes). */
    var horizontal: Boolean = false
        set(value) { field = value; requestLayout(); invalidate() }

    private var barras: List<Barra> = emptyList()

    private fun dp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, resources.displayMetrics)
    private fun sp(v: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, resources.displayMetrics)

    private fun colorTema(attr: Int, fallback: Int): Int {
        val tv = TypedValue()
        return if (context.theme.resolveAttribute(attr, tv, true)) tv.data else fallback
    }

    private val paintBarra = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTema(R.attr.colorPrimary, 0xFF3F51B5.toInt())
    }
    private val paintTexto = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorTema(com.google.android.material.R.attr.colorOnSurface, 0xFF222222.toInt())
        textSize = sp(11f)
    }
    private val paintLinea = Paint().apply {
        color = 0x22000000
        strokeWidth = dp(1f)
    }

    fun setDatos(nuevas: List<Barra>) {
        barras = nuevas
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = if (horizontal) (barras.size * dp(34f) + dp(8f)).toInt().coerceAtLeast(dp(60f).toInt())
        else dp(220f).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        if (barras.isEmpty()) return
        val maxValor = max(barras.maxOf { it.valor }, 1.0)
        if (horizontal) dibujarHorizontal(canvas, maxValor) else dibujarVertical(canvas, maxValor)
    }

    private fun dibujarVertical(canvas: Canvas, maxValor: Double) {
        val izq = dp(8f); val der = width - dp(8f)
        val arriba = dp(22f); val abajo = height - dp(24f)
        canvas.drawLine(izq, abajo, der, abajo, paintLinea)
        val ranura = (der - izq) / barras.size
        val anchoBarra = ranura * 0.6f
        paintTexto.textAlign = Paint.Align.CENTER
        barras.forEachIndexed { i, b ->
            val cx = izq + ranura * i + ranura / 2
            val alto = ((abajo - arriba) * (b.valor / maxValor)).toFloat()
            canvas.drawRoundRect(RectF(cx - anchoBarra / 2, abajo - alto, cx + anchoBarra / 2, abajo), dp(4f), dp(4f), paintBarra)
            canvas.drawText(formatear(b.valor), cx, abajo - alto - dp(4f), paintTexto)
            val etiqueta = TextUtils.ellipsize(b.etiqueta, paintTexto, ranura, TextUtils.TruncateAt.END).toString()
            canvas.drawText(etiqueta, cx, height - dp(7f), paintTexto)
        }
    }

    private fun dibujarHorizontal(canvas: Canvas, maxValor: Double) {
        val ancho = width.toFloat()
        val zonaEtiqueta = ancho * 0.34f
        val zonaValor = dp(56f)
        val inicioBarra = zonaEtiqueta + dp(8f)
        val maxAncho = ancho - inicioBarra - zonaValor
        val fila = dp(34f)
        val altoBarra = dp(16f)
        barras.forEachIndexed { i, b ->
            val cy = dp(4f) + fila * i + fila / 2
            paintTexto.textAlign = Paint.Align.LEFT
            val etiqueta = TextUtils.ellipsize(b.etiqueta, paintTexto, zonaEtiqueta, TextUtils.TruncateAt.END).toString()
            canvas.drawText(etiqueta, 0f, cy + paintTexto.textSize / 3, paintTexto)
            val largo = (maxAncho * (b.valor / maxValor)).toFloat().coerceAtLeast(dp(3f))
            canvas.drawRoundRect(RectF(inicioBarra, cy - altoBarra / 2, inicioBarra + largo, cy + altoBarra / 2), dp(4f), dp(4f), paintBarra)
            canvas.drawText(formatear(b.valor), inicioBarra + largo + dp(6f), cy + paintTexto.textSize / 3, paintTexto)
        }
    }

    private fun formatear(v: Double) =
        if (v >= 1000) String.format(Locale.US, "%.1fk", v / 1000)
        else String.format(Locale.US, "%.0f", v)
}
