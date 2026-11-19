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

    private val vistosMap = mutableMapOf<String, Long>()
    private var currentBaseline: Boolean = false

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun getKey(usuarioId: Int): String = "usuario_$usuarioId"
    private fun getVistosKey(usuarioId: Int): String = "vistos_$usuarioId"
    private fun getBaselineKey(usuarioId: Int): String = "baseline_$usuarioId"

    fun tieneBaseline(usuarioId: Int): Boolean {
        if ((usuarioId == -1) || !::prefs.isInitialized) return false
        if (usuarioId == currentUsuarioId) return currentBaseline
        return prefs.getBoolean(getBaselineKey(usuarioId), false)
    }

    fun marcarBaseline(usuarioId: Int) {
        if ((usuarioId == -1) || !::prefs.isInitialized) return
        currentBaseline = true
        try {
            prefs.edit { putBoolean(getBaselineKey(usuarioId), true) }
        } catch (_: Exception) {}
    }

    fun cargarPara(usuarioId: Int) {
        if (usuarioId == -1) return
        currentUsuarioId = usuarioId
        if (!::prefs.isInitialized) return

        currentBaseline = prefs.getBoolean(getBaselineKey(usuarioId), false)

        vistosMap.clear()
        val jsonVistos = prefs.getString(getVistosKey(usuarioId), null)
        if (!jsonVistos.isNullOrBlank()) {
            try {
                val type = object : TypeToken<Map<String, Long>>() {}.type
                val parsed: Map<String, Long>? = gson.fromJson(jsonVistos, type)
                parsed?.let { vistosMap.putAll(it) }
            } catch (_: Exception) {}
        }

        val ahora = System.currentTimeMillis()
        val limite90Dias = ahora - (90L * 24 * 3600 * 1000L)
        val iterator = vistosMap.entries.iterator()
        var sePodo = false
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value < limite90Dias) {
                iterator.remove()
                sePodo = true
            }
        }
        if (sePodo && ::prefs.isInitialized) {
            persistirVistos(usuarioId)
        }

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

    fun esEventoNuevo(evento: EventoNotificacion, ahora: Long): Boolean {
        val limite90Dias = ahora - (90L * 24 * 3600 * 1000L)
        return (!vistosMap.containsKey(evento.id)) && (evento.fechaMillis >= limite90Dias)
    }

    fun registrarVistos(nuevosVistos: Map<String, Long>) {
        if (currentUsuarioId == -1 || nuevosVistos.isEmpty()) return
        vistosMap.putAll(nuevosVistos)
        if (::prefs.isInitialized) {
            persistirVistos(currentUsuarioId)
        }
    }

    private fun persistirVistos(usuarioId: Int) {
        try {
            val json = gson.toJson(vistosMap)
            prefs.edit { putString(getVistosKey(usuarioId), json) }
        } catch (_: Exception) {}
    }

    fun agregarNuevas(eventos: List<EventoNotificacion>, leidasDefault: Boolean): List<NotificacionApp> {
        if (currentUsuarioId == -1 || eventos.isEmpty()) return emptyList()
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
            if (::prefs.isInitialized) {
                persistir(currentUsuarioId, sorted)
            }
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
        if (::prefs.isInitialized) {
            persistir(currentUsuarioId, list)
        }
    }

    fun marcarTodasLeidas() {
        if (currentUsuarioId == -1) return
        val list = _notificaciones.value.map { it.copy(leida = true) }
        _notificaciones.value = list
        _noLeidas.value = 0
        if (::prefs.isInitialized) {
            persistir(currentUsuarioId, list)
        }
    }

    private fun persistir(usuarioId: Int, list: List<NotificacionApp>) {
        try {
            val json = gson.toJson(list)
            prefs.edit { putString(getKey(usuarioId), json) }
        } catch (_: Exception) {}
    }
}
