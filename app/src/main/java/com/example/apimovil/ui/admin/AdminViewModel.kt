package com.example.apimovil.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apimovil.data.ApiClient
import com.example.apimovil.data.models.*
import com.example.apimovil.ui.common.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

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

    // Verificación de contraseña de administrador
    private val _verificacion = MutableStateFlow(UiState<Boolean>())
    val verificacion = _verificacion.asStateFlow()

    fun verificarContrasena(contrasena: String, alExito: () -> Unit) {
        viewModelScope.launch {
            _verificacion.value = UiState(loading = true)
            try {
                val respuesta = ApiClient.api.verificarContrasena(VerificarContrasenaRequest(contrasena))
                if (respuesta.permitido) {
                    _verificacion.value = UiState(data = true)
                    alExito()
                } else {
                    _verificacion.value = UiState(error = respuesta.mensaje ?: "Contraseña incorrecta.")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val interp = interpretarError(e)
                _verificacion.value = UiState(error = interp.mensaje, camposError = interp.camposError)
            }
        }
    }

    fun limpiarVerificacion() {
        _verificacion.value = UiState()
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

    private val _conteosOrdenes = MutableStateFlow(UiState<Triple<Int, Int, Int>>())
    val conteosOrdenes = _conteosOrdenes.asStateFlow()

    fun cargarConteosOrdenes() {
        viewModelScope.launch {
            _conteosOrdenes.value = UiState(loading = true, data = _conteosOrdenes.value.data)
            try {
                val resAbiertas = ApiClient.api.listarOrdenes(estado = "abierta,abierta_tarde")
                val resCompletadas = ApiClient.api.listarOrdenes(estado = "completada,completada_tarde")
                val resVencidas = ApiClient.api.listarOrdenes(estado = "vencida")

                val totalAbiertas = resAbiertas.total ?: resAbiertas.data.size
                val totalCompletadas = resCompletadas.total ?: resCompletadas.data.size
                val totalVencidas = resVencidas.total ?: resVencidas.data.size

                _conteosOrdenes.value = UiState(data = Triple(totalAbiertas, totalCompletadas, totalVencidas))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _conteosOrdenes.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    // Rehabilita una orden vencida
    private val _rehabilitacion = MutableStateFlow(UiState<Boolean>())
    val rehabilitacion = _rehabilitacion.asStateFlow()

    fun rehabilitarOrden(ordenId: Int, horas: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _rehabilitacion.value = UiState(loading = true)
            try {
                ApiClient.api.rehabilitarOrden(ordenId, RehabilitarOrdenRequest(horas))
                _rehabilitacion.value = UiState(data = true)
                onSuccess()
                cargarOrdenes()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _rehabilitacion.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun limpiarRehabilitacion() {
        _rehabilitacion.value = UiState()
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
                _vehiculos.value = UiState(data = ApiClient.api.listarVehiculos(todos = 1).data)
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

    // Reportes de consumo
    var periodoSeleccionadoEtiqueta: String = "Este mes"
    var desdePeriodo: String? = obtenerInicioMesString()
    var hastaPeriodo: String? = obtenerFechaActualString()

    var conductorFiltroId: Int? = null
    var conductorFiltroNombre: String? = null
    var gasolineraFiltroId: Int? = null
    var gasolineraFiltroNombre: String? = null

    private val _reporteConductor = MutableStateFlow(UiState<List<ReporteConductor>>())
    val reporteConductor = _reporteConductor.asStateFlow()

    private val _reporteGasolinera = MutableStateFlow(UiState<List<ReporteGasolinera>>())
    val reporteGasolinera = _reporteGasolinera.asStateFlow()

    private val _reporteResumen = MutableStateFlow(UiState<ReporteResumenResp>())
    val reporteResumen = _reporteResumen.asStateFlow()

    private val _detalleConductor = MutableStateFlow(UiState<ReporteConductorDetalleResp>())
    val detalleConductor = _detalleConductor.asStateFlow()

    private val _detalleGasolinera = MutableStateFlow(UiState<ReporteGasolineraDetalleResp>())
    val detalleGasolinera = _detalleGasolinera.asStateFlow()

    private val _conductoresSinFiltro = MutableStateFlow<List<ReporteConductor>>(emptyList())
    val conductoresSinFiltro = _conductoresSinFiltro.asStateFlow()

    private val _gasolinerasSinFiltro = MutableStateFlow<List<ReporteGasolinera>>(emptyList())
    val gasolinerasSinFiltro = _gasolinerasSinFiltro.asStateFlow()

    fun cargarListasSinFiltro(desde: String? = desdePeriodo, hasta: String? = hastaPeriodo) {
        viewModelScope.launch {
            try {
                _conductoresSinFiltro.value = ApiClient.api.reportePorConductor(desde, hasta, null)
            } catch (_: Exception) {}
            try {
                _gasolinerasSinFiltro.value = ApiClient.api.reportePorGasolinera(desde, hasta, null)
            } catch (_: Exception) {}
        }
    }

    fun cargarReportePorConductor(desde: String? = desdePeriodo, hasta: String? = hastaPeriodo) {
        desdePeriodo = desde
        hastaPeriodo = hasta
        viewModelScope.launch {
            _reporteConductor.value = UiState(loading = true)
            try {
                val lista = ApiClient.api.reportePorConductor(desde, hasta, conductorFiltroId)
                _reporteConductor.value = UiState(data = lista)
                if (conductorFiltroId == null) {
                    _conductoresSinFiltro.value = lista
                } else if (_conductoresSinFiltro.value.isEmpty()) {
                    _conductoresSinFiltro.value = ApiClient.api.reportePorConductor(desde, hasta, null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _reporteConductor.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun cargarReportePorGasolinera(desde: String? = desdePeriodo, hasta: String? = hastaPeriodo) {
        desdePeriodo = desde
        hastaPeriodo = hasta
        viewModelScope.launch {
            _reporteGasolinera.value = UiState(loading = true)
            try {
                val lista = ApiClient.api.reportePorGasolinera(desde, hasta, gasolineraFiltroId)
                _reporteGasolinera.value = UiState(data = lista)
                if (gasolineraFiltroId == null) {
                    _gasolinerasSinFiltro.value = lista
                } else if (_gasolinerasSinFiltro.value.isEmpty()) {
                    _gasolinerasSinFiltro.value = ApiClient.api.reportePorGasolinera(desde, hasta, null)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _reporteGasolinera.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun cargarReporteResumen(desde: String? = desdePeriodo, hasta: String? = hastaPeriodo, porConductor: Boolean) {
        desdePeriodo = desde
        hastaPeriodo = hasta
        viewModelScope.launch {
            _reporteResumen.value = UiState(loading = true)
            try {
                _reporteResumen.value = UiState(
                    data = ApiClient.api.reporteResumen(
                        desde = desde,
                        hasta = hasta,
                        conductorId = if (porConductor) conductorFiltroId else null,
                        gasolineraId = if (!porConductor) gasolineraFiltroId else null
                    )
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _reporteResumen.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun cargarDetalleConductor(id: Int, desde: String? = desdePeriodo, hasta: String? = hastaPeriodo) {
        desdePeriodo = desde
        hastaPeriodo = hasta
        viewModelScope.launch {
            _detalleConductor.value = UiState(loading = true)
            try {
                _detalleConductor.value = UiState(data = ApiClient.api.reporteDetalleConductor(id, desde, hasta))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _detalleConductor.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun cargarDetalleGasolinera(id: Int, desde: String? = desdePeriodo, hasta: String? = hastaPeriodo) {
        desdePeriodo = desde
        hastaPeriodo = hasta
        viewModelScope.launch {
            _detalleGasolinera.value = UiState(loading = true)
            try {
                _detalleGasolinera.value = UiState(data = ApiClient.api.reporteDetalleGasolinera(id, desde, hasta))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _detalleGasolinera.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    private val _estimacionVehiculo = MutableStateFlow(UiState<EstimacionCombustible>())
    val estimacionVehiculo = _estimacionVehiculo.asStateFlow()

    private val _estimacionMgp = MutableStateFlow(UiState<EstimacionCombustible>())
    val estimacionMgp = _estimacionMgp.asStateFlow()

    private var jobEstimarVehiculo: Job? = null
    private var jobEstimarMgp: Job? = null

    fun estimarVehiculo(monto: Double, vehiculoId: Int) {
        if (monto <= 0) {
            jobEstimarVehiculo?.cancel()
            _estimacionVehiculo.value = UiState()
            return
        }
        jobEstimarVehiculo?.cancel()
        jobEstimarVehiculo = viewModelScope.launch {
            _estimacionVehiculo.value = UiState(loading = true)
            try {
                delay(400)
                val res = ApiClient.api.estimarCombustible(EstimacionRequest(monto = monto, vehiculo_id = vehiculoId))
                _estimacionVehiculo.value = UiState(data = res)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estimacionVehiculo.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    fun estimarMgp(monto: Double) {
        if (monto <= 0) {
            jobEstimarMgp?.cancel()
            _estimacionMgp.value = UiState()
            return
        }
        jobEstimarMgp?.cancel()
        jobEstimarMgp = viewModelScope.launch {
            _estimacionMgp.value = UiState(loading = true)
            try {
                delay(400)
                val res = ApiClient.api.estimarCombustible(EstimacionRequest(monto = monto, tipo_combustible = "gasolina"))
                _estimacionMgp.value = UiState(data = res)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _estimacionMgp.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    private val _precios = MutableStateFlow(UiState<List<PrecioCombustible>>())
    val precios = _precios.asStateFlow()

    fun cargarPrecios() {
        viewModelScope.launch {
            _precios.value = UiState(loading = true)
            try {
                val lista = ApiClient.api.preciosCombustible("san_pedro_sula")
                _precios.value = UiState(data = lista)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _precios.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    private val _historialPrecios = MutableStateFlow(UiState<HistorialPrecios>())
    val historialPrecios = _historialPrecios.asStateFlow()

    fun cargarHistorialPrecios() {
        viewModelScope.launch {
            _historialPrecios.value = UiState(loading = true)
            try {
                val hp = ApiClient.api.historialPrecios(12)
                _historialPrecios.value = UiState(data = hp)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _historialPrecios.value = UiState(error = mensajeDeError(e))
            }
        }
    }

    // Gestion de catalogos (crear, editar, eliminar, reactivar)
    var ultimoTipoAccion: String = "exito" // "exito" o "reactivado"
    private val _accionCatalogo = MutableStateFlow(UiState<Boolean>())
    val accionCatalogo = _accionCatalogo.asStateFlow()

    fun limpiarAccionCatalogo() {
        _accionCatalogo.value = UiState()
    }

    fun crearConductor(datos: Map<String, Any?>) {
        ultimoTipoAccion = "exito"
        viewModelScope.launch {
            _accionCatalogo.value = UiState(loading = true)
            try {
                ApiClient.api.crearConductor(datos)
                _accionCatalogo.value = UiState(data = true)
                cargarConductores()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val interp = interpretarError(e)
                _accionCatalogo.value = UiState(error = interp.mensaje, camposError = interp.camposError)
            }
        }
    }

    fun crearVehiculo(datos: Map<String, Any?>) {
        ultimoTipoAccion = "exito"
        viewModelScope.launch {
            _accionCatalogo.value = UiState(loading = true)
            try {
                ApiClient.api.crearVehiculo(datos)
                _accionCatalogo.value = UiState(data = true)
                cargarVehiculos()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val interp = interpretarError(e)
                _accionCatalogo.value = UiState(error = interp.mensaje, camposError = interp.camposError)
            }
        }
    }

    fun crearGasolinera(datos: Map<String, Any?>) {
        ultimoTipoAccion = "exito"
        viewModelScope.launch {
            _accionCatalogo.value = UiState(loading = true)
            try {
                ApiClient.api.crearGasolinera(datos)
                _accionCatalogo.value = UiState(data = true)
                cargarGasolineras()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val interp = interpretarError(e)
                _accionCatalogo.value = UiState(error = interp.mensaje, camposError = interp.camposError)
            }
        }
    }

    fun desactivarConductor(id: Int) {
        ultimoTipoAccion = "exito"
        viewModelScope.launch {
            _accionCatalogo.value = UiState(loading = true)
            try {
                ApiClient.api.desactivarConductor(id)
                _accionCatalogo.value = UiState(data = true)
                cargarConductores()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val interp = interpretarError(e)
                _accionCatalogo.value = UiState(error = interp.mensaje, camposError = interp.camposError)
            }
        }
    }

    fun desactivarVehiculo(id: Int) {
        ultimoTipoAccion = "exito"
        viewModelScope.launch {
            _accionCatalogo.value = UiState(loading = true)
            try {
                ApiClient.api.desactivarVehiculo(id)
                _accionCatalogo.value = UiState(data = true)
                cargarVehiculos()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val interp = interpretarError(e)
                _accionCatalogo.value = UiState(error = interp.mensaje, camposError = interp.camposError)
            }
        }
    }

    fun desactivarGasolinera(id: Int) {
        ultimoTipoAccion = "exito"
        viewModelScope.launch {
            _accionCatalogo.value = UiState(loading = true)
            try {
                ApiClient.api.desactivarGasolinera(id)
                _accionCatalogo.value = UiState(data = true)
                cargarGasolineras()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val interp = interpretarError(e)
                _accionCatalogo.value = UiState(error = interp.mensaje, camposError = interp.camposError)
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
                val interp = interpretarError(e)
                _accionCatalogo.value = UiState(error = interp.mensaje, camposError = interp.camposError)
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
                val interp = interpretarError(e)
                _accionCatalogo.value = UiState(error = interp.mensaje, camposError = interp.camposError)
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
                val interp = interpretarError(e)
                _accionCatalogo.value = UiState(error = interp.mensaje, camposError = interp.camposError)
            }
        }
    }

    fun reactivarConductor(id: Int) {
        ultimoTipoAccion = "reactivado"
        editarConductor(id, mapOf("activo" to true))
    }

    fun reactivarVehiculo(id: Int) {
        ultimoTipoAccion = "reactivado"
        editarVehiculo(id, mapOf("activo" to true))
    }

    fun reactivarGasolinera(id: Int) {
        ultimoTipoAccion = "reactivado"
        editarGasolinera(id, mapOf("activo" to true))
    }
}
