package com.example.apimovil.ui.conductor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apimovil.data.ApiClient
import com.example.apimovil.data.models.EscanearRequest
import com.example.apimovil.data.models.MensajeResponse
import com.example.apimovil.data.models.Orden
import com.example.apimovil.data.models.Solicitud
import com.example.apimovil.data.models.SolicitudRequest
import com.example.apimovil.data.models.Vehiculo
import com.example.apimovil.ui.common.UiState
import com.example.apimovil.ui.common.comprimirFoto
import com.example.apimovil.ui.common.mensajeDeError
import com.google.gson.Gson
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
import retrofit2.HttpException
import java.io.File

// ViewModel para las pantallas del flujo del conductor
class ConductorViewModel : ViewModel() {

    private val _vehiculos = MutableStateFlow(UiState<List<Vehiculo>>())
    val vehiculos = _vehiculos.asStateFlow()

    private val _ordenes = MutableStateFlow(UiState<List<Orden>>())
    val ordenes = _ordenes.asStateFlow()

    private val _solicitud = MutableStateFlow(UiState<Boolean>())
    val solicitud = _solicitud.asStateFlow()

    private val _edicionSolicitud = MutableStateFlow(UiState<Boolean>())
    val edicionSolicitud = _edicionSolicitud.asStateFlow()

    private val _solicitudDetalle = MutableStateFlow(UiState<Solicitud>())
    val solicitudDetalle = _solicitudDetalle.asStateFlow()

    private val _solicitudes = MutableStateFlow(UiState<List<Solicitud>>())
    val solicitudes = _solicitudes.asStateFlow()

    private val _ordenEscaneada = MutableStateFlow(UiState<Orden>())
    val ordenEscaneada = _ordenEscaneada.asStateFlow()

    private val _evidencia = MutableStateFlow(UiState<Orden>())
    val evidencia = _evidencia.asStateFlow()

    // Carga los vehiculos disponibles
    fun cargarVehiculos() {
        viewModelScope.launch {
            _vehiculos.value = UiState(loading = true, data = _vehiculos.value.data)
            try {
                val pagina = ApiClient.api.listarVehiculos()
                _vehiculos.value = UiState(data = pagina.data)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _vehiculos.value = UiState(data = _vehiculos.value.data, error = mensajeDeError(e))
            }
        }
    }

    // Carga las ordenes asignadas al conductor
    fun cargarMisOrdenes() {
        viewModelScope.launch {
            _ordenes.value = UiState(loading = true, data = _ordenes.value.data)
            try {
                val pagina = ApiClient.api.listarOrdenes()
                _ordenes.value = UiState(data = pagina.data)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ordenes.value = UiState(data = _ordenes.value.data, error = mensajeDeError(e))
            }
        }
    }

    // Crea una nueva solicitud de combustible
    fun crearSolicitud(
        tipoDestino: String,
        vehiculoId: Int?,
        descripcionMgp: String?,
        descripcionActividad: String
    ) {
        if (descripcionActividad.isBlank()) {
            _solicitud.value = UiState(error = "Describe la actividad que justifica el combustible.")
            return
        }

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
                _solicitud.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun cargarSolicitudPorId(id: Int) {
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
        if (descripcionActividad.isBlank()) {
            _edicionSolicitud.value = UiState(error = "Describe la actividad que justifica el combustible.")
            return
        }

        viewModelScope.launch {
            _edicionSolicitud.value = UiState(loading = true)
            try {
                ApiClient.api.editarSolicitud(
                    id,
                    SolicitudRequest(
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
                _edicionSolicitud.value = UiState(error = mensajeDeError(e))
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
        viewModelScope.launch {
            _ordenEscaneada.value = UiState(loading = true)
            try {
                val orden = ApiClient.api.escanearOrden(EscanearRequest(codigoQr))
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

    // Carga el detalle de una orden por su identificador
    fun cargarOrdenPorId(id: Int) {
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
    fun subirEvidencia(detalleId: Int, tipo: String, justificacion: String?, archivoFoto: File, ordenIdParaRefrescar: Int? = null) {
        viewModelScope.launch {
            var archivoComprimidoTemporal: File? = null

            try {
                _evidencia.value = UiState(loading = true)

                // Comprimir la foto en un hilo secundario (IO) antes de construir el multipart
                val (fotoComp, esTemp) = withContext(Dispatchers.IO) {
                    try {
                        val tempDir = archivoFoto.parentFile ?: File(archivoFoto.path).parentFile
                        Pair(comprimirFoto(archivoFoto, tempDir), true)
                    } catch (_: Exception) {
                        Pair(archivoFoto, false) // Respaldo: subir el original si falla la compresion
                    }
                }

                val archivoAEnviar = fotoComp
                if (esTemp) {
                    archivoComprimidoTemporal = fotoComp
                }

                val cuerpoTipo = tipo.toRequestBody("text/plain".toMediaTypeOrNull())
                val cuerpoJustificacion = justificacion?.takeIf { it.isNotBlank() }?.toRequestBody("text/plain".toMediaTypeOrNull())
                val cuerpoFoto = archivoAEnviar.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val parteFoto = MultipartBody.Part.createFormData("foto", archivoAEnviar.name, cuerpoFoto)
                val orden = ApiClient.api.subirEvidencia(detalleId, cuerpoTipo, cuerpoJustificacion, parteFoto)
                _evidencia.value = UiState(data = orden)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (e is HttpException && e.code() == 422) {
                    val cuerpo = e.response()?.errorBody()?.string()
                    val mensaje = try {
                        val parseado = Gson().fromJson(cuerpo, MensajeResponse::class.java)
                        parseado.mensaje ?: parseado.message ?: ""
                    } catch (_: Exception) { "" }

                    if (mensaje.contains("Ese tipo de evidencia ya fue subido", ignoreCase = true) || mensaje.contains("ya fue subido", ignoreCase = true)) {
                        if (ordenIdParaRefrescar != null) {
                            try {
                                val ordenRefrescada = ApiClient.api.verOrden(ordenIdParaRefrescar)
                                _evidencia.value = UiState(data = ordenRefrescada)
                                return@launch
                            } catch (_: Exception) {}
                        }
                    }
                }
                _evidencia.value = UiState(error = mensajeDeError(e))
            } finally {
                // Borra el archivo comprimido temporal al terminar la subida (exito o error)
                archivoComprimidoTemporal?.delete()
            }
        }
    }

    fun limpiarEvidencia() {
        _evidencia.value = UiState()
    }
}
