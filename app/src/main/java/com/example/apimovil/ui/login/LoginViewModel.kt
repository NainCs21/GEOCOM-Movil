package com.example.apimovil.ui.login

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
        if (correo.isBlank() || contrasena.isBlank()) {
            _state.value = UiState(error = "Completa correo y contrasena.")
            return
        }

        viewModelScope.launch {
            _state.value = UiState(loading = true)
            try {
                val respuesta = ApiClient.api.login(LoginRequest(correo.trim(), contrasena))
                SessionManager.guardarSesion(
                    token = respuesta.token,
                    usuarioId = respuesta.usuario.id,
                    nombre = respuesta.usuario.nombre,
                    correo = respuesta.usuario.correo,
                    rol = respuesta.usuario.rol
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
