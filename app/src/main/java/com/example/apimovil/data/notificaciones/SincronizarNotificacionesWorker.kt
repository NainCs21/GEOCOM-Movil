package com.example.apimovil.data.notificaciones

import android.content.Context
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

        if (SessionManager.haySesion && SessionManager.haExpiradoInactividad()) {
            SessionManager.guardarMensajeSalida("Tu sesión se cerró por inactividad.")
            SessionManager.cerrarSesionRemotaYLocal()
            cancelarSincronizacionNotificaciones(applicationContext)
            return Result.success()
        }

        if (!SessionManager.haySesion || !SessionManager.puedeConsultar) {
            return Result.success()
        }

        return try {
            NotificacionesRepository.sincronizar(applicationContext)
            Result.success()
        } catch (_: Exception) {
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
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    } catch (_: Exception) {}
}

fun cancelarSincronizacionNotificaciones(context: Context) {
    try {
        WorkManager.getInstance(context).cancelUniqueWork("geocom_sync_notif")
    } catch (_: Exception) {}
}
