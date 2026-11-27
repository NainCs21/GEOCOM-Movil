package com.example.apimovil.ui.conductor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apimovil.data.ApiClient
import com.example.apimovil.data.models.*
import com.example.apimovil.ui.common.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class ConductorViewModel : ViewModel() {

    private val _ordenes = MutableStateFlow(UiState<List<Orden>>())
    val ordenes = _ordenes.asStateFlow()

    private val _solicitudes = MutableStateFlow(UiState<List<Solicitud>>())
    val solicitudes = _solicitudes.asStateFlow()

    private val _solicitudDetalle = MutableStateFlow(UiState<Solicitud>())
    val solicitudDetalle = _solicitudDetalle.asStateFlow()

    private val _solicitud = MutableStateFlow(UiState<Boolean>())
    val solicitud = _solicitud.asStateFlow()

    private val _edicionSolicitud = MutableStateFlow(UiState<Boolean>())
    val edicionSolicitud = _edicionSolicitud.asStateFlow()

    private val _ordenEscaneada = MutableStateFlow(UiState<Orden>())
    val ordenEscaneada = _ordenEscaneada.asStateFlow()

    private val _evidencia = MutableStateFlow(UiState<Orden>())
    val evidencia = _evidencia.asStateFlow()

    private val _vehiculos = MutableStateFlow(UiState<List<Vehiculo>>())
    val vehiculos = _vehiculos.asStateFlow()

    fun cargarVehiculos() {
        viewModelScope.launch {
            _vehiculos.value = UiState(loading = true)
            try {
                _vehiculos.value = UiState(data = ApiClient.api.listarVehiculos().data)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _vehiculos.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun cargarMisOrdenes() {
        viewModelScope.launch {
            _ordenes.value = UiState(loading = true, data = _ordenes.value.data)
            try {
                val lista = ApiClient.api.listarOrdenes().data
                _ordenes.value = UiState(data = lista)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ordenes.value = UiState(data = _ordenes.value.data, error = mensajeDeError(e))
            }
        }
    }

    fun crearSolicitud(
        tipoDestino: String,
        vehiculoId: Int?,
        descripcionMgp: String?,
        descripcionActividad: String
    ) {
        if (_solicitud.value.loading) return

        viewModelScope.launch {
            _solicitud.value = UiState(loading = true)
            try {
                ApiClient.api.crearSolicitud(
                    SolicitudRequest(
                        tipo_destino = tipoDestino,
                        vehiculo_id = vehiculoId,
                        descripcion_mgp = descripcionMgp,
                        descripcion_actividad = descripcionActividad
                    )
                )
                _solicitud.value = UiState(data = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val interp = interpretarError(e)
                _solicitud.value = UiState(error = interp.mensaje, camposError = interp.camposError)
            }
        }
    }

    fun cargarSolicitudPorId(id: Int) {
        if (!Validadores.idValido(id)) return
        viewModelScope.launch {
            _solicitudDetalle.value = UiState(loading = true)
            try {
                val sol = ApiClient.api.verSolicitud(id)
                _solicitudDetalle.value = UiState(data = sol)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _solicitudDetalle.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun editarSolicitud(
        id: Int,
        tipoDestino: String,
        vehiculoId: Int?,
        descripcionMgp: String?,
        descripcionActividad: String
    ) {
        if (!Validadores.idValido(id) || _edicionSolicitud.value.loading) return

        viewModelScope.launch {
            _edicionSolicitud.value = UiState(loading = true)
            try {
                ApiClient.api.editarSolicitud(
                    id = id,
                    body = SolicitudRequest(
                        tipo_destino = tipoDestino,
                        vehiculo_id = vehiculoId,
                        descripcion_mgp = descripcionMgp,
                        descripcion_actividad = descripcionActividad
                    )
                )
                _edicionSolicitud.value = UiState(data = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val interp = interpretarError(e)
                _edicionSolicitud.value = UiState(error = interp.mensaje, camposError = interp.camposError)
            }
        }
    }

    fun limpiarEdicionSolicitud() {
        _edicionSolicitud.value = UiState()
    }

    fun cargarMisSolicitudes() {
        viewModelScope.launch {
            _solicitudes.value = UiState(loading = true, data = _solicitudes.value.data)
            try {
                val pagina = ApiClient.api.listarSolicitudes()
                _solicitudes.value = UiState(data = pagina.data)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _solicitudes.value = UiState(data = _solicitudes.value.data, error = mensajeDeError(e))
            }
        }
    }

    // Valida el codigo QR de la gasolinera
    fun escanear(codigoQr: String) {
        if (_ordenEscaneada.value.loading) return

        val valQr = Validadores.codigoQrEscaneado(codigoQr)
        if (!valQr.esOk) {
            _ordenEscaneada.value = UiState(error = valQr.mensajeError())
            return
        }

        val qrLimpio = valQr.valorOrNull()!!

        viewModelScope.launch {
            _ordenEscaneada.value = UiState(loading = true)
            try {
                val orden = ApiClient.api.escanearOrden(EscanearRequest(qrLimpio))
                _ordenEscaneada.value = UiState(data = orden)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ordenEscaneada.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun limpiarEscaneo() {
        _ordenEscaneada.value = UiState()
    }

    fun cargarOrdenPorId(id: Int) {
        if (!Validadores.idValido(id)) return
        viewModelScope.launch {
            _ordenEscaneada.value = UiState(loading = true)
            try {
                val orden = ApiClient.api.verOrden(id)
                _ordenEscaneada.value = UiState(data = orden)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ordenEscaneada.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun subirEvidencia(detalleId: Int, archivoFoto: File, ordenIdParaRefrescar: Int? = null) {
        subirEvidencia(detalleId, "bomba", null, archivoFoto, ordenIdParaRefrescar)
    }

    // Envia la foto de evidencia al servidor
    fun subirEvidencia(
        detalleId: Int,
        tipo: String,
        justificacion: String?,
        archivoFoto: File,
        ordenIdParaRefrescar: Int? = null
    ) {
        if (_evidencia.value.loading) return

        viewModelScope.launch {
            var archivoComprimidoTemporal: File? = null

            try {
                _evidencia.value = UiState(loading = true)

                val (fotoComp, errorComp) = withContext(Dispatchers.IO) {
                    try {
                        val tempDir = File(archivoFoto.parentFile ?: File(archivoFoto.path).parentFile, ".")
                        Pair(comprimirFoto(archivoFoto, tempDir), null)
                    } catch (e: Exception) {
                        Pair(null, e.message ?: "No se pudo procesar la foto. Vuelve a tomarla.")
                    }
                }

                if (fotoComp == null) {
                    _evidencia.value = UiState(error = errorComp ?: "No se pudo procesar la foto. Vuelve a tomarla.")
                    return@launch
                }
                archivoComprimidoTemporal = fotoComp

                val cuerpoTipo = tipo.toRequestBody("text/plain".toMediaTypeOrNull())
                val cuerpoJustificacion = justificacion?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
                val cuerpoFoto = fotoComp.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val parteFoto = MultipartBody.Part.createFormData("foto", "evidencia.jpg", cuerpoFoto)

                val orden = ApiClient.api.subirEvidencia(detalleId, cuerpoTipo, cuerpoJustificacion, parteFoto)
                _evidencia.value = UiState(data = orden)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val interp = interpretarError(e)
                if (interp.codigo == 422 || interp.mensaje.contains("ya fue subido", ignoreCase = true)) {
                    if (ordenIdParaRefrescar != null) {
                        try {
                            val ordenRefrescada = ApiClient.api.verOrden(ordenIdParaRefrescar)
                            _evidencia.value = UiState(data = ordenRefrescada)
                            return@launch
                        } catch (_: Exception) {}
                    }
                }
                _evidencia.value = UiState(error = interp.mensaje)
            } finally {
                try { archivoFoto.delete() } catch (_: Exception) {}
                try { archivoComprimidoTemporal?.delete() } catch (_: Exception) {}
            }
        }
    }

    fun limpiarEvidencia() {
        _evidencia.value = UiState()
    }
}
