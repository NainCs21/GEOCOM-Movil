package com.example.apimovil.data.notificaciones

import android.content.Context
import com.example.apimovil.data.SessionManager
import kotlinx.coroutines.sync.Mutex

object NotificacionesRepository {
    private val fuente: FuenteNotificaciones = FuenteDerivada()
    private val mutex = Mutex()

    suspend fun sincronizar(context: Context) {
        if (!SessionManager.haySesion || !SessionManager.puedeConsultar) return
        val usuarioId = SessionManager.usuarioId
        if (usuarioId == -1) return

        if (!mutex.tryLock()) {
            return
        }

        try {
            NotificacionesStore.init(context)
            NotificacionesStore.cargarPara(usuarioId)

            val eventos = fuente.obtenerEventos()
            val ahora = System.currentTimeMillis()
            val tieneBaseline = NotificacionesStore.tieneBaseline(usuarioId)

            if (!tieneBaseline) {
                val vistosMap = eventos.associateBy({ it.id }, { it.fechaMillis })
                NotificacionesStore.registrarVistos(vistosMap)

                val limite24h = ahora - (24 * 3600 * 1000L)
                val eventos24h = eventos.filter { it.fechaMillis >= limite24h }
                NotificacionesStore.agregarNuevas(eventos24h, leidasDefault = true)

                NotificacionesStore.marcarBaseline(usuarioId)
            } else {
                val eventosNuevos = eventos.filter { NotificacionesStore.esEventoNuevo(it, ahora) }
                if (eventosNuevos.isNotEmpty()) {
                    val vistosNuevos = eventosNuevos.associateBy({ it.id }, { it.fechaMillis })
                    NotificacionesStore.registrarVistos(vistosNuevos)

                    val agregadas = NotificacionesStore.agregarNuevas(eventosNuevos, leidasDefault = false)
                    if (agregadas.isNotEmpty()) {
                        if (agregadas.size > 5) {
                            Notificador.mostrarResumen(context, agregadas.size)
                        } else {
                            for (nueva in agregadas) {
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
                    }
                }
            }
        } catch (_: Exception) {
            // Si falla cualquier llamada de red, sale sin modificar el estado
        } finally {
            mutex.unlock()
        }
    }
}
