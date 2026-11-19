package com.example.apimovil.data.notificaciones

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.apimovil.MainActivity
import com.example.apimovil.R
import com.example.apimovil.data.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NotificacionesForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        SessionManager.init(applicationContext)

        if (!SessionManager.haySesion || !SessionManager.puedeConsultar) {
            detenerServicioLocal()
            return START_NOT_STICKY
        }

        Notificador.crearCanales(applicationContext)

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, Notificador.CHANNEL_SERVICIO)
            .setContentTitle("GEOCOM")
            .setContentText("Monitoreando solicitudes y órdenes en segundo plano")
            .setSmallIcon(R.drawable.ic_bell)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        iniciarPolling()

        return START_STICKY
    }

    private fun iniciarPolling() {
        if (pollingJob?.isActive == true) return

        pollingJob = serviceScope.launch {
            while (isActive) {
                if (!SessionManager.haySesion || !SessionManager.puedeConsultar) {
                    detenerServicioLocal()
                    break
                }
                try {
                    NotificacionesRepository.sincronizar(applicationContext)
                } catch (e: Exception) {
                    Log.e("GeoComService", "Error durante sincronización en segundo plano", e)
                }
                delay(30_000L)
            }
        }
    }

    private fun detenerServicioLocal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 9991

        fun iniciar(context: Context) {
            val intent = Intent(context, NotificacionesForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun detener(context: Context) {
            val intent = Intent(context, NotificacionesForegroundService::class.java)
            context.stopService(intent)
        }
    }
}
