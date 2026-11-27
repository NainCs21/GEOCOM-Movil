package com.example.apimovil.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apimovil.data.ApiClient
import com.example.apimovil.data.SessionManager
import com.example.apimovil.data.models.LoginRequest
import com.example.apimovil.ui.common.UiState
import com.example.apimovil.ui.common.Validadores
import com.example.apimovil.ui.common.interpretarError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel para la pantalla de inicio de sesion
class LoginViewModel : ViewModel() {

    private val _state = MutableStateFlow(UiState<Boolean>())
    val state = _state.asStateFlow()

    // Realiza la autenticacion del usuario con la API
    fun login(correoInput: String?, contrasenaInput: String?) {
        val resCorreo = Validadores.correo(correoInput)
        if (!resCorreo.esOk) {
            _state.value = UiState(error = resCorreo.mensajeError())
            return
        }

        val resContrasena = Validadores.contrasenaLogin(contrasenaInput)
        if (!resContrasena.esOk) {
            _state.value = UiState(error = resContrasena.mensajeError())
            return
        }

        val correoLimpio = resCorreo.valorOrNull()!!
        val contrasenaLimpia = resContrasena.valorOrNull()!!

        viewModelScope.launch {
            _state.value = UiState(loading = true)
            try {
                val respuesta = ApiClient.api.login(LoginRequest(correoLimpio, contrasenaLimpia))
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
                val interp = interpretarError(e)
                _state.value = UiState(error = interp.mensaje, camposError = interp.camposError)
            }
        }
    }
}
