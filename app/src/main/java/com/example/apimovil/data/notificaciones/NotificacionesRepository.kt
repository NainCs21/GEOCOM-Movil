package com.example.apimovil.data.notificaciones

import android.content.Context
import com.example.apimovil.data.SessionManager

object NotificacionesRepository {
    private val fuente: FuenteNotificaciones = FuenteDerivada()

    suspend fun sincronizar(context: Context) {
        if (!SessionManager.haySesion || !SessionManager.puedeConsultar) return
        val usuarioId = SessionManager.usuarioId
        if (usuarioId == -1) return

        NotificacionesStore.cargarPara(usuarioId)

        try {
            val eventos = fuente.obtenerEventos()
            val storeActual = NotificacionesStore.notificaciones.value

            if (storeActual.isEmpty()) {
                val ahora = System.currentTimeMillis()
                val limite24h = ahora - (24 * 3600 * 1000L)
                val eventosRecientes = eventos.filter { it.fechaMillis >= limite24h }
                NotificacionesStore.agregarNuevas(eventosRecientes, leidasDefault = true)
            } else {
                val nuevas = NotificacionesStore.agregarNuevas(eventos, leidasDefault = false)
                for (nueva in nuevas) {
                    val evento = EventoNotificacion(
                        id = nueva.id,
                        tipo = nueva.tipo,
                        titulo = nueva.titulo,
                        cuerpo = nueva.cuerpo,
                        fechaMillis = nueva.fechaMillis,
                        destino = nueva.destino,
                        ordenId = nueva.ordenId,
                    )
                    Notificador.mostrar(context, evento)
                }
            }
        } catch (_: Exception) {}
    }
}
