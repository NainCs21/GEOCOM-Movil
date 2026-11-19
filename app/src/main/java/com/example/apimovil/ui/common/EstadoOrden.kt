package com.example.apimovil.ui.common

// Helpers para evaluar los estados actualizados de las ordenes en el backend
fun esAbierta(e: String?): Boolean = e == "abierta" || e == "abierta_tarde"

fun esCompletada(e: String?): Boolean = e == "completada" || e == "completada_tarde"

fun esTarde(e: String?): Boolean = e == "abierta_tarde" || e == "completada_tarde"
