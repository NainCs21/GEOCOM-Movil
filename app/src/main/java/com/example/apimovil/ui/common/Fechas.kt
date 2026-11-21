package com.example.apimovil.ui.common

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Normaliza y parsea fechas recibidas de la API (ISO 8601 UTC o MySQL Datetime).
 */
fun parseFechaIso(fecha: String): Date {
    if (fecha.isBlank()) return Date()

    val normalizada = fecha.trim().replace(Regex("\\.(\\d{3})\\d*Z?$"), ".$1Z")

    val patrones = arrayOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd",
    )

    for (patron in patrones) {
        try {
            val sdf = SimpleDateFormat(patron, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(if (patron.contains("'Z'")) normalizada else fecha.trim())
            if (date != null) return date
        } catch (_: Exception) {}
    }
    return Date()
}

fun parseFechaIsoMillis(fecha: String?): Long {
    if (fecha.isNullOrBlank()) return 0L
    return try {
        parseFechaIso(fecha).time
    } catch (_: Exception) {
        0L
    }
}

fun formatoLegible(fecha: String): String {
    return try {
        val formato = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale("es", "HN"))
        formato.format(parseFechaIso(fecha))
    } catch (_: Exception) {
        fecha
    }
}

fun obtenerFechaActualString(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
}

fun obtenerHace3DiasString(): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -3)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
}

fun obtenerInicioMesString(): String {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
}

fun obtenerMesAnteriorRango(): Pair<String, String> {
    val cal = Calendar.getInstance()
    cal.add(Calendar.MONTH, -1)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val desde = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    val hasta = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    return Pair(desde, hasta)
}

fun obtenerUltimos7DiasRango(): Pair<String, String> {
    val cal = Calendar.getInstance()
    val hasta = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    cal.add(Calendar.DAY_OF_YEAR, -6)
    val desde = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    return Pair(desde, hasta)
}

fun formatearFechaIsoShort(fechaStr: String?): String {
    if (fechaStr.isNullOrBlank()) return ""
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("es", "HN"))
        val date = parser.parse(fechaStr)
        if (date != null) formatter.format(date) else fechaStr
    } catch (_: Exception) {
        fechaStr
    }
}

fun formatoMoneda(monto: Number?): String {
    val valor = monto?.toDouble() ?: 0.0
    val symbols = DecimalFormatSymbols(Locale("es", "HN")).apply {
        decimalSeparator = '.'
        groupingSeparator = ','
    }
    val formatter = DecimalFormat("#,##0.00", symbols)
    return "L ${formatter.format(valor)}"
}

fun formatoCargo(cargo: String?): String {
    return when (cargo?.lowercase()?.trim()) {
        "tecnico" -> "Técnico"
        "tecnico_jefe_cuadrilla", "jefe_cuadrilla" -> "Jefe de cuadrilla"
        else -> cargo?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "Técnico"
    }
}
