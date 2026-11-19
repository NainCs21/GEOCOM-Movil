package com.example.apimovil.data.notificaciones

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.apimovil.data.SessionManager

class NotificacionesBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i("GeoComBoot", "NotificacionesBootReceiver recibido accion: $action")
        if (Intent.ACTION_BOOT_COMPLETED == action || Intent.ACTION_MY_PACKAGE_REPLACED == action) {
            SessionManager.init(context)
            Notificador.crearCanales(context)

            if (SessionManager.haySesion && SessionManager.puedeConsultar) {
                NotificacionesForegroundService.iniciar(context)
                programarSincronizacionNotificaciones(context)
            }
        }
    }
}
