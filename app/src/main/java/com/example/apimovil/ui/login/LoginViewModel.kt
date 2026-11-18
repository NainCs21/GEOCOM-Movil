package com.example.apimovil.ui.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apimovil.data.ApiClient
import com.example.apimovil.data.SessionManager
import com.example.apimovil.data.models.LoginRequest
import com.example.apimovil.ui.common.UiState
import com.example.apimovil.ui.common.mensajeDeError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel para la pantalla de inicio de sesion
class LoginViewModel : ViewModel() {

    private val _state = MutableStateFlow(UiState<Boolean>())
    val state = _state.asStateFlow()

    // Realiza la autenticacion del usuario con la API
    fun login(correo: String, contrasena: String) {
        val correoTrim = correo.trim()
        if (correoTrim.isBlank()) {
            _state.value = UiState(error = "Escribe tu correo.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correoTrim).matches()) {
            _state.value = UiState(error = "Escribe un correo válido, por ejemplo nombre@empresa.com.")
            return
        }
        if (contrasena.isBlank()) {
            _state.value = UiState(error = "Escribe tu contraseña.")
            return
        }

        viewModelScope.launch {
            _state.value = UiState(loading = true)
            try {
                val respuesta = ApiClient.api.login(LoginRequest(correoTrim, contrasena))
                SessionManager.guardarSesion(
                    token = respuesta.token,
                    usuarioId = respuesta.usuario.id,
                    nombre = respuesta.usuario.nombre,
                    correo = respuesta.usuario.correo,
                    rol = respuesta.usuario.rol,
                )
                _state.value = UiState(data = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = UiState(error = mensajeDeError(e))
            }
        }
    }
}
