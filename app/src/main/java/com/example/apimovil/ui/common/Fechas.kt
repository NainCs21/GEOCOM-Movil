package com.example.apimovil.ui.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Laravel serializa sus datetime como ISO 8601 en UTC con microsegundos,
 * ej. "2026-11-11T09:12:00.000000Z". SimpleDateFormat solo entiende
 * milisegundos (3 dígitos), así que se recortan los microsegundos antes
 * de parsear.
 */
fun parseFechaIso(fecha: String): Date {
    val normalizada = fecha.replace(Regex("\\.(\\d{3})\\d*Z$"), ".$1Z")
    val formato = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    formato.timeZone = TimeZone.getTimeZone("UTC")
    return formato.parse(normalizada) ?: Date()
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
