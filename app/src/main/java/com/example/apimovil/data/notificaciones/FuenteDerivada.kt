package com.example.apimovil.data.notificaciones

import com.example.apimovil.data.ApiClient
import com.example.apimovil.data.models.Orden
import com.example.apimovil.data.models.Solicitud
import com.example.apimovil.ui.common.formatoLegible
import com.example.apimovil.ui.common.obtenerHace3DiasString
import com.example.apimovil.ui.common.parseFechaIsoMillis
import java.util.Locale

class FuenteDerivada : FuenteNotificaciones {
    override suspend fun obtenerEventos(): List<EventoNotificacion> {
        val limite3DiasMs = System.currentTimeMillis() - (3L * 24 * 3600 * 1000L)

        val solicitudes = mutableListOf<Solicitud>()
        var page = 1
        var lastPage = 1
        do {
            val resp = ApiClient.api.listarSolicitudes(page = page)
            solicitudes.addAll(resp.data)
            lastPage = resp.last_page ?: 1

            val todosViejos = resp.data.isNotEmpty() && resp.data.all {
                parseFechaIsoMillis(it.created_at) < limite3DiasMs
            }
            if (todosViejos) break
            page++
        } while (page <= lastPage)

        val ordenes = mutableListOf<Orden>()
        val fechaHace3Dias = obtenerHace3DiasString()
        page = 1
        lastPage = 1
        do {
            val resp = ApiClient.api.listarOrdenes(desde = fechaHace3Dias, page = page)
            ordenes.addAll(resp.data)
            lastPage = resp.last_page ?: 1
            page++
        } while (page <= lastPage)

        return derivarEventos(solicitudes, ordenes)
    }
}

fun derivarEventos(solicitudes: List<Solicitud>, ordenes: List<Orden>): List<EventoNotificacion> {
    val lista = mutableListOf<EventoNotificacion>()

    for (sol in solicitudes) {
        val conductorNombre = sol.conductor?.usuario?.name ?: "Conductor"
        // 1. solicitud_nueva
        if (!sol.created_at.isNullOrBlank()) {
            lista.add(
                EventoNotificacion(
                    id = "solicitud_nueva_${sol.id}",
                    tipo = "solicitud_nueva",
                    titulo = "Nueva solicitud",
                    cuerpo = "$conductorNombre solicitó combustible (${sol.tipo_destino})",
                    fechaMillis = parseFechaIsoMillis(sol.created_at),
                    destino = "solicitudes",
                    ordenId = null,
                ),
            )
        }
        // 3. solicitud_rechazada
        if (sol.estado == "rechazada") {
            val fechaStr = sol.updated_at ?: sol.created_at
            if (!fechaStr.isNullOrBlank()) {
                lista.add(
                    EventoNotificacion(
                        id = "solicitud_rechazada_${sol.id}",
                        tipo = "solicitud_rechazada",
                        titulo = "Solicitud rechazada",
                        cuerpo = "La solicitud de $conductorNombre fue rechazada",
                        fechaMillis = parseFechaIsoMillis(fechaStr),
                        destino = "solicitudes",
                        ordenId = null,
                    ),
                )
            }
        }
    }

    for (ord in ordenes) {
        val conductorNombre = ord.conductor?.usuario?.name ?: "Conductor"
        val gasolineraNombre = ord.gasolinera?.nombre ?: ""
        val total = ord.detalles?.sumOf { it.monto } ?: 0.0
        val totalFmt = String.format(Locale.US, "%.2f", total)

        // 2. orden_asignada
        if (!ord.fecha_asignacion.isNullOrBlank()) {
            lista.add(
                EventoNotificacion(
                    id = "orden_asignada_${ord.id}",
                    tipo = "orden_asignada",
                    titulo = "Solicitud aceptada",
                    cuerpo = "Se asignó la orden ${ord.numero_orden} a $conductorNombre en $gasolineraNombre. Total: L $totalFmt",
                    fechaMillis = parseFechaIsoMillis(ord.fecha_asignacion),
                    destino = "orden",
                    ordenId = ord.id,
                ),
            )
        }

        // 4. orden_abierta
        if (!ord.fecha_apertura.isNullOrBlank()) {
            val horaVence = if (!ord.vence_apertura.isNullOrBlank()) formatoLegible(ord.vence_apertura) else ""
            val cuerpoTexto = if (horaVence.isNotBlank()) {
                "$conductorNombre abrió la orden ${ord.numero_orden} en $gasolineraNombre. Tiene hasta las $horaVence para subir las evidencias"
            } else {
                "$conductorNombre abrió la orden ${ord.numero_orden} en $gasolineraNombre"
            }
            lista.add(
                EventoNotificacion(
                    id = "orden_abierta_${ord.id}",
                    tipo = "orden_abierta",
                    titulo = "Orden abierta",
                    cuerpo = cuerpoTexto,
                    fechaMillis = parseFechaIsoMillis(ord.fecha_apertura),
                    destino = "orden",
                    ordenId = ord.id,
                ),
            )
        }

        // 6. orden_completada
        if (!ord.fecha_completada.isNullOrBlank()) {
            lista.add(
                EventoNotificacion(
                    id = "orden_completada_${ord.id}",
                    tipo = "orden_completada",
                    titulo = "Orden completada",
                    cuerpo = "$conductorNombre completó la orden ${ord.numero_orden} en $gasolineraNombre. Total: L $totalFmt",
                    fechaMillis = parseFechaIsoMillis(ord.fecha_completada),
                    destino = "orden",
                    ordenId = ord.id,
                ),
            )
        }

        // 7. orden_vencida
        if (ord.estado == "vencida") {
            val fechaVenceStr = if (ord.fecha_apertura != null) ord.vence_apertura else ord.vence_asignacion
            if (!fechaVenceStr.isNullOrBlank()) {
                lista.add(
                    EventoNotificacion(
                        id = "orden_vencida_${ord.id}",
                        tipo = "orden_vencida",
                        titulo = "Orden vencida",
                        cuerpo = "La orden ${ord.numero_orden} de $conductorNombre venció sin completarse",
                        fechaMillis = parseFechaIsoMillis(fechaVenceStr),
                        destino = "orden",
                        ordenId = ord.id,
                    ),
                )
            }
        }
    }

    return lista.sortedByDescending { it.fechaMillis }
}
