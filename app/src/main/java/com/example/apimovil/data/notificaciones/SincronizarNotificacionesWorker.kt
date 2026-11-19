package com.example.apimovil.data.notificaciones

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.apimovil.data.SessionManager
import java.util.concurrent.TimeUnit

class SincronizarNotificacionesWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        SessionManager.init(applicationContext)

        val haySesion = SessionManager.haySesion
        val rol = SessionManager.rol
        val usuarioId = SessionManager.usuarioId
        Log.i("GeoComSync", "doWork iniciado: haySesion=$haySesion, rol=$rol, usuarioId=$usuarioId")

        if (!haySesion || !SessionManager.puedeConsultar) {
            Log.w("GeoComSync", "doWork salida temprana: haySesion=$haySesion, puedeConsultar=${SessionManager.puedeConsultar}")
            return Result.success()
        }

        return try {
            NotificacionesRepository.sincronizar(applicationContext)
            Log.i("GeoComSync", "doWork completado con éxito")
            Result.success()
        } catch (e: Exception) {
            Log.w("GeoComSync", "doWork falló con excepción", e)
            Result.success()
        }
    }
}

fun programarSincronizacionNotificaciones(context: Context) {
    if (!SessionManager.puedeConsultar) return
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
    val request = PeriodicWorkRequestBuilder<SincronizarNotificacionesWorker>(15, TimeUnit.MINUTES)
        .setConstraints(constraints)
        .build()
    try {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "geocom_sync_notif",
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
        Log.i("GeoComSync", "Sincronización de notificaciones programada")
    } catch (e: Exception) {
        Log.w("GeoComSync", "Error al programar sincronización de notificaciones", e)
    }
}

fun cancelarSincronizacionNotificaciones(context: Context) {
    try {
        WorkManager.getInstance(context).cancelUniqueWork("geocom_sync_notif")
        Log.i("GeoComSync", "Sincronización de notificaciones cancelada")
    } catch (e: Exception) {
        Log.w("GeoComSync", "Error al cancelar sincronización de notificaciones", e)
    }
}
