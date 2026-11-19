package com.example.apimovil.data.notificaciones

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.apimovil.MainActivity
import com.example.apimovil.R

object Notificador {
    const val CHANNEL_EVENTOS = "geocom_eventos"
    const val CHANNEL_EVIDENCIAS = "geocom_evidencias"

    var isNotificacionesScreenActive = false

    fun crearCanales(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channelEventos = NotificationChannel(
                CHANNEL_EVENTOS,
                "Solicitudes y órdenes",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notificaciones de nuevas solicitudes, órdenes y estados"
            }

            val channelEvidencias = NotificationChannel(
                CHANNEL_EVIDENCIAS,
                "Evidencias subidas",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Notificaciones cuando se suben evidencias fotográficas"
            }

            notificationManager.createNotificationChannel(channelEventos)
            notificationManager.createNotificationChannel(channelEvidencias)
        }
    }

    fun mostrar(context: Context, evento: EventoNotificacion) {
        if (isNotificacionesScreenActive) return

        val channelId = if (evento.tipo == "evidencia_subida") CHANNEL_EVIDENCIAS else CHANNEL_EVENTOS

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notif_id", evento.id)
            putExtra("notif_destino", evento.destino)
            if (evento.ordenId != null) {
                putExtra("notif_orden_id", evento.ordenId)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            evento.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle(evento.titulo)
            .setContentText(evento.cuerpo)
            .setStyle(NotificationCompat.BigTextStyle().bigText(evento.cuerpo))
            .setPriority(if (evento.tipo == "evidencia_subida") NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            if ((Build.VERSION.SDK_INT < 33) || (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED)
            ) {
                NotificationManagerCompat.from(context).notify(evento.id.hashCode(), builder.build())
            }
        } catch (_: Exception) {}
    }
}
