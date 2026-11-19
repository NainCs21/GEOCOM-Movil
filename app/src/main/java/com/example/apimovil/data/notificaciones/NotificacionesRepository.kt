package com.example.apimovil.data.notificaciones

import android.content.Context
import android.util.Log
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

            val storeActual = NotificacionesStore.notificaciones.value
            val eventos = fuente.obtenerEventos()
            val ahora = System.currentTimeMillis()
            val tieneBaseline = NotificacionesStore.tieneBaseline(usuarioId)

            var nuevas: List<NotificacionApp> = emptyList()

            if (!tieneBaseline) {
                val vistosMap = eventos.associateBy({ it.id }, { it.fechaMillis })
                NotificacionesStore.registrarVistos(vistosMap)

                val limite24h = ahora - (24 * 3600 * 1000L)
                val eventos24h = eventos.filter { it.fechaMillis >= limite24h }
                nuevas = NotificacionesStore.agregarNuevas(eventos24h, leidasDefault = true)

                NotificacionesStore.marcarBaseline(usuarioId)
            } else {
                val eventosNuevos = eventos.filter { NotificacionesStore.esEventoNuevo(it, ahora) }
                if (eventosNuevos.isNotEmpty()) {
                    val vistosNuevos = eventosNuevos.associateBy({ it.id }, { it.fechaMillis })
                    NotificacionesStore.registrarVistos(vistosNuevos)

                    val agregadas = NotificacionesStore.agregarNuevas(eventosNuevos, leidasDefault = false)
                    nuevas = agregadas
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

            Log.i("GeoComSync", "eventos=${eventos.size} nuevos=${nuevas.size} baseline=${storeActual.isEmpty()}")
        } catch (e: Exception) {
            Log.w("GeoComSync", "sincronizar falló", e)
        } finally {
            mutex.unlock()
        }
    }
}
