package com.example.apimovil

import android.app.Application
import com.example.apimovil.data.SessionManager
import com.example.apimovil.data.notificaciones.NotificacionesForegroundService
import com.example.apimovil.data.notificaciones.NotificacionesStore
import com.example.apimovil.data.notificaciones.Notificador
import com.example.apimovil.data.notificaciones.programarSincronizacionNotificaciones

class GeoComApp : Application() {

    companion object {
        @Volatile
        var arranqueEnFrio: Boolean = true
    }

    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
        NotificacionesStore.init(this)
        Notificador.crearCanales(this)

        if (SessionManager.haySesion && SessionManager.puedeConsultar) {
            NotificacionesForegroundService.iniciar(this)
            programarSincronizacionNotificaciones(this)
        }
    }
}
