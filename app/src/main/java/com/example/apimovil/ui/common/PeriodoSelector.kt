package com.example.apimovil.ui.common

import androidx.fragment.app.Fragment
import com.example.apimovil.R
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun mostrarSelectorPeriodo(
    fragment: Fragment,
    onElegido: (desde: String?, hasta: String?, etiqueta: String) -> Unit
) {
    val context = fragment.requireContext()
    val opciones = arrayOf(
        context.getString(R.string.periodo_todo),
        context.getString(R.string.periodo_este_mes),
        context.getString(R.string.periodo_mes_anterior),
        context.getString(R.string.periodo_ultimos_7_dias),
        context.getString(R.string.periodo_personalizado),
    )
    MaterialAlertDialogBuilder(context)
        .setTitle("Seleccionar periodo")
        .setItems(opciones) { _, which ->
            when (which) {
                0 -> {
                    onElegido(null, null, context.getString(R.string.periodo_todo))
                }
                1 -> {
                    val desde = obtenerInicioMesString()
                    val hasta = obtenerFechaActualString()
                    onElegido(desde, hasta, context.getString(R.string.periodo_este_mes))
                }
                2 -> {
                    val (desdeMesAnt, hastaMesAnt) = obtenerMesAnteriorRango()
                    val etiqueta = "${formatearFechaIsoShort(desdeMesAnt)} - ${formatearFechaIsoShort(hastaMesAnt)}"
                    onElegido(desdeMesAnt, hastaMesAnt, etiqueta)
                }
                3 -> {
                    val (desde7d, hasta7d) = obtenerUltimos7DiasRango()
                    val etiqueta = "${formatearFechaIsoShort(desde7d)} - ${formatearFechaIsoShort(hasta7d)}"
                    onElegido(desde7d, hasta7d, etiqueta)
                }
                4 -> {
                    val picker = MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Seleccionar rango de fechas")
                        .build()
                    picker.addOnPositiveButtonClickListener { selection ->
                        val desde = convertirMillisAFecha(selection.first)
                        val hasta = convertirMillisAFecha(selection.second)
                        val etiqueta = "${formatearFechaIsoShort(desde)} - ${formatearFechaIsoShort(hasta)}"
                        onElegido(desde, hasta, etiqueta)
                    }
                    picker.show(fragment.childFragmentManager, "dateRangePicker")
                }
            }
        }
        .show()
}

private fun convertirMillisAFecha(millis: Long): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return formatter.format(Date(millis))
}
