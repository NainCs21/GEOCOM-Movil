package com.example.apimovil.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apimovil.data.ApiClient
import com.example.apimovil.data.models.*
import com.example.apimovil.ui.common.UiState
import com.example.apimovil.ui.common.mensajeDeError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ViewModel principal para las funciones de administracion y supervision
class AdminViewModel : ViewModel() {

    // Solicitudes pendientes de asignar monto
    private val _solicitudesPendientes = MutableStateFlow(UiState<List<Solicitud>>())
    val solicitudesPendientes = _solicitudesPendientes.asStateFlow()

    // Carga la lista de solicitudes pendientes
    fun cargarSolicitudesPendientes() {
        viewModelScope.launch {
            _solicitudesPendientes.value = UiState(loading = true, data = _solicitudesPendientes.value.data)
            try {
                val pagina = ApiClient.api.listarSolicitudes()
                _solicitudesPendientes.value = UiState(data = pagina.data.filter { it.estado == "pendiente" })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _solicitudesPendientes.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    private val _rechazoSolicitud = MutableStateFlow(UiState<Boolean>())
    val rechazoSolicitud = _rechazoSolicitud.asStateFlow()

    // Rechaza una solicitud de combustible
    fun rechazarSolicitud(id: Int) {
        viewModelScope.launch {
            _rechazoSolicitud.value = UiState(loading = true)
            try {
                ApiClient.api.rechazarSolicitud(id)
                _rechazoSolicitud.value = UiState(data = true)
                cargarSolicitudesPendientes()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _rechazoSolicitud.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    // Asignacion de monto y creacion de la orden
    private val _asignacion = MutableStateFlow(UiState<Boolean>())
    val asignacion = _asignacion.asStateFlow()

    // Asigna el monto a una solicitud y genera la orden
    fun asignarOrden(
        solicitudId: Int,
        gasolineraId: Int,
        horasVigencia: Int,
        detalles: List<DetalleLineaRequest>
    ) {
        viewModelScope.launch {
            _asignacion.value = UiState(loading = true)
            try {
                ApiClient.api.crearOrden(
                    AsignarOrdenRequest(solicitudId, gasolineraId, horasVigencia, detalles)
                )
                _asignacion.value = UiState(data = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _asignacion.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    // Consulta general de ordenes
    private val _ordenes = MutableStateFlow(UiState<List<Orden>>())
    val ordenes = _ordenes.asStateFlow()

    // Carga las ordenes con filtro opcional por estado, conductor, gasolinera o rango de fechas
    fun cargarOrdenes(
        estado: String? = null,
        conductorId: Int? = null,
        gasolineraId: Int? = null,
        desde: String? = null,
        hasta: String? = null
    ) {
        viewModelScope.launch {
            _ordenes.value = UiState(loading = true, data = _ordenes.value.data)
            try {
                val pagina = ApiClient.api.listarOrdenes(
                    estado = estado,
                    conductorId = conductorId,
                    gasolineraId = gasolineraId,
                    desde = desde,
                    hasta = hasta
                )
                _ordenes.value = UiState(data = pagina.data)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ordenes.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    // Rehabilita una orden vencida
    fun rehabilitarOrden(ordenId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                ApiClient.api.rehabilitarOrden(ordenId)
                onSuccess()
                cargarOrdenes()
            } catch (e: Exception) {
                _ordenes.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    // Catalogos de conductores, vehiculos y gasolineras
    private val _conductores = MutableStateFlow(UiState<List<Conductor>>())
    val conductores = _conductores.asStateFlow()

    fun cargarConductores() {
        viewModelScope.launch {
            _conductores.value = UiState(loading = true)
            try {
                _conductores.value = UiState(data = ApiClient.api.listarConductores().data)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _conductores.value = UiState(error = mensajeDeError(e))
            }
        }
    }

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

    private val _gasolineras = MutableStateFlow(UiState<List<Gasolinera>>())
    val gasolineras = _gasolineras.asStateFlow()

    fun cargarGasolineras() {
        viewModelScope.launch {
            _gasolineras.value = UiState(loading = true)
            try {
                _gasolineras.value = UiState(data = ApiClient.api.listarGasolineras().data)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _gasolineras.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    // Registros de caja chica
    private val _cajaChica = MutableStateFlow(UiState<List<CajaChica>>())
    val cajaChica = _cajaChica.asStateFlow()

    fun cargarCajaChica() {
        viewModelScope.launch {
            _cajaChica.value = UiState(loading = true)
            try {
                _cajaChica.value = UiState(data = ApiClient.api.listarCajaChica().data)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _cajaChica.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    private val _registroCajaChica = MutableStateFlow(UiState<Boolean>())
    val registroCajaChica = _registroCajaChica.asStateFlow()

    fun registrarCajaChica(conductorId: Int?, monto: Double, descripcion: String, fechaEntrega: String) {
        viewModelScope.launch {
            _registroCajaChica.value = UiState(loading = true)
            try {
                ApiClient.api.registrarCajaChica(
                    CajaChicaRequest(conductorId, monto, descripcion, fechaEntrega)
                )
                _registroCajaChica.value = UiState(data = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _registroCajaChica.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    // Reportes de consumo
    private val _reporteConductor = MutableStateFlow(UiState<List<ReporteConductor>>())
    val reporteConductor = _reporteConductor.asStateFlow()

    private val _reporteGasolinera = MutableStateFlow(UiState<List<ReporteGasolinera>>())
    val reporteGasolinera = _reporteGasolinera.asStateFlow()

    fun cargarReportePorConductor() {
        viewModelScope.launch {
            _reporteConductor.value = UiState(loading = true)
            try {
                _reporteConductor.value = UiState(data = ApiClient.api.reportePorConductor())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _reporteConductor.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun cargarReportePorGasolinera() {
        viewModelScope.launch {
            _reporteGasolinera.value = UiState(loading = true)
            try {
                _reporteGasolinera.value = UiState(data = ApiClient.api.reportePorGasolinera())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _reporteGasolinera.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    // Gestion de catalogos (crear, editar, eliminar)
    private val _accionCatalogo = MutableStateFlow(UiState<Boolean>())
    val accionCatalogo = _accionCatalogo.asStateFlow()

    fun crearConductor(datos: Map<String, Any?>) {
        viewModelScope.launch {
            _accionCatalogo.value = UiState(loading = true)
            try {
                ApiClient.api.crearConductor(datos)
                _accionCatalogo.value = UiState(data = true)
                cargarConductores()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _accionCatalogo.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun crearVehiculo(datos: Map<String, Any?>) {
        viewModelScope.launch {
            _accionCatalogo.value = UiState(loading = true)
            try {
                ApiClient.api.crearVehiculo(datos)
                _accionCatalogo.value = UiState(data = true)
                cargarVehiculos()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _accionCatalogo.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun crearGasolinera(datos: Map<String, Any?>) {
        viewModelScope.launch {
            _accionCatalogo.value = UiState(loading = true)
            try {
                ApiClient.api.crearGasolinera(datos)
                _accionCatalogo.value = UiState(data = true)
                cargarGasolineras()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _accionCatalogo.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun eliminarConductor(id: Int) {
        viewModelScope.launch {
            _accionCatalogo.value = UiState(loading = true)
            try {
                ApiClient.api.desactivarConductor(id)
                _accionCatalogo.value = UiState(data = true)
                cargarConductores()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _accionCatalogo.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun eliminarVehiculo(id: Int) {
        viewModelScope.launch {
            _accionCatalogo.value = UiState(loading = true)
            try {
                ApiClient.api.desactivarVehiculo(id)
                _accionCatalogo.value = UiState(data = true)
                cargarVehiculos()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _accionCatalogo.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun eliminarGasolinera(id: Int) {
        viewModelScope.launch {
            _accionCatalogo.value = UiState(loading = true)
            try {
                ApiClient.api.desactivarGasolinera(id)
                _accionCatalogo.value = UiState(data = true)
                cargarGasolineras()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _accionCatalogo.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun editarConductor(id: Int, datos: Map<String, Any?>) {
        viewModelScope.launch {
            _accionCatalogo.value = UiState(loading = true)
            try {
                ApiClient.api.editarConductor(id, datos)
                _accionCatalogo.value = UiState(data = true)
                cargarConductores()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _accionCatalogo.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun editarVehiculo(id: Int, datos: Map<String, Any?>) {
        viewModelScope.launch {
            _accionCatalogo.value = UiState(loading = true)
            try {
                ApiClient.api.editarVehiculo(id, datos)
                _accionCatalogo.value = UiState(data = true)
                cargarVehiculos()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _accionCatalogo.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun editarGasolinera(id: Int, datos: Map<String, Any?>) {
        viewModelScope.launch {
            _accionCatalogo.value = UiState(loading = true)
            try {
                ApiClient.api.editarGasolinera(id, datos)
                _accionCatalogo.value = UiState(data = true)
                cargarGasolineras()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _accionCatalogo.value = UiState(error = mensajeDeError(e))
            }
        }
    }
}
