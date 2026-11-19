package com.example.apimovil.data.notificaciones

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificacionesStore {
    private const val PREFS_NAME = "geocom_notificaciones"
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    private var currentUsuarioId: Int = -1

    private val _notificaciones = MutableStateFlow<List<NotificacionApp>>(emptyList())
    val notificaciones: StateFlow<List<NotificacionApp>> = _notificaciones.asStateFlow()

    private val _noLeidas = MutableStateFlow(0)
    val noLeidas: StateFlow<Int> = _noLeidas.asStateFlow()

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getKey(usuarioId: Int): String = "usuario_$usuarioId"

    fun cargarPara(usuarioId: Int) {
        currentUsuarioId = usuarioId
        val json = prefs.getString(getKey(usuarioId), null)
        val list = if (json.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                val type = object : TypeToken<List<NotificacionApp>>() {}.type
                gson.fromJson<List<NotificacionApp>>(json, type) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        }
        val sorted = list.sortedByDescending { it.fechaMillis }
        _notificaciones.value = sorted
        _noLeidas.value = sorted.count { !it.leida }
    }

    fun agregarNuevas(eventos: List<EventoNotificacion>, leidasDefault: Boolean): List<NotificacionApp> {
        if (currentUsuarioId == -1) return emptyList()
        val existing = _notificaciones.value.toMutableList()
        val existingIds = existing.map { it.id }.toSet()

        val nuevas = mutableListOf<NotificacionApp>()
        for (ev in eventos) {
            if (ev.id !in existingIds) {
                val notif = NotificacionApp(
                    id = ev.id,
                    tipo = ev.tipo,
                    titulo = ev.titulo,
                    cuerpo = ev.cuerpo,
                    fechaMillis = ev.fechaMillis,
                    destino = ev.destino,
                    ordenId = ev.ordenId,
                    leida = leidasDefault,
                )
                nuevas.add(notif)
            }
        }

        if (nuevas.isNotEmpty()) {
            existing.addAll(nuevas)
            val sorted = existing.sortedByDescending { it.fechaMillis }.take(200)
            _notificaciones.value = sorted
            _noLeidas.value = sorted.count { !it.leida }
            persistir(currentUsuarioId, sorted)
        }
        return nuevas
    }

    fun marcarLeida(id: String) {
        if (currentUsuarioId == -1) return
        val list = _notificaciones.value.map {
            if (it.id == id) it.copy(leida = true) else it
        }
        _notificaciones.value = list
        _noLeidas.value = list.count { !it.leida }
        persistir(currentUsuarioId, list)
    }

    fun marcarTodasLeidas() {
        if (currentUsuarioId == -1) return
        val list = _notificaciones.value.map { it.copy(leida = true) }
        _notificaciones.value = list
        _noLeidas.value = 0
        persistir(currentUsuarioId, list)
    }

    private fun persistir(usuarioId: Int, list: List<NotificacionApp>) {
        try {
            val json = gson.toJson(list)
            prefs.edit { putString(getKey(usuarioId), json) }
        } catch (_: Exception) {}
    }
}
