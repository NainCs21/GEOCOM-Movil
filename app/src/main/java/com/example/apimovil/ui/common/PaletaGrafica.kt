package com.example.apimovil.ui.common

import kotlin.math.abs

object PaletaGrafica {
    private val colores = intArrayOf(
        0xFF6042A6.toInt(), 0xFFE8590C.toInt(), 0xFF0C8599.toInt(), 0xFFD6336C.toInt(),
        0xFF2F9E44.toInt(), 0xFF1C7ED6.toInt(), 0xFFC99700.toInt(), 0xFFA0522D.toInt(),
        0xFF495057.toInt(), 0xFFE03131.toInt(), 0xFF5C940D.toInt(), 0xFFAE3EC9.toInt()
    )

    /** Un color por id, sin repetir colores dentro de la lista (hasta 12 elementos). */
    fun asignar(ids: List<Int>): List<Int> {
        val usados = BooleanArray(colores.size)
        var ocupados = 0
        return ids.map { id ->
            var idx = abs(id) % colores.size
            if (ocupados < colores.size) {
                while (usados[idx]) idx = (idx + 1) % colores.size
                usados[idx] = true
                ocupados++
            }
            colores[idx]
        }
    }
}
