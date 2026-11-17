package com.example.apimovil.ui.common

import java.text.SimpleDateFormat
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
