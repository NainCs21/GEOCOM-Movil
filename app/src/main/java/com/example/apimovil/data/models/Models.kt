package com.example.apimovil.data.models

// Modelos de datos para inicio de sesion y autenticacion
data class LoginRequest(
    val correo: String,
    val contrasena: String,
    val nombre_dispositivo: String? = "app-movil"
)

data class LoginResponse(
    val token: String,
    val usuario: UsuarioInfo
)

data class UsuarioInfo(
    val id: Int,
    val nombre: String,
    val correo: String,
    val rol: String?
)

// Modelos para catalogos de la empresa
data class Conductor(
    val id: Int,
    val usuario_id: Int,
    val telefono: String,
    val cargo: String,
    val activo: Boolean,
    val usuario: UsuarioResumen? = null
)

data class UsuarioResumen(
    val id: Int,
    val name: String,
    val email: String? = null
)

data class Vehiculo(
    val id: Int,
    val placa: String,
    val marca: String,
    val modelo: String,
    val anio: Int?,
    val tipo_combustible: String,
    val activo: Boolean
)

data class Gasolinera(
    val id: Int,
    val nombre: String,
    val direccion: String?,
    val codigo_qr: String,
    val activo: Boolean
)

// Modelos para solicitudes de combustible
data class SolicitudRequest(
    val tipo_destino: String,
    val vehiculo_id: Int? = null,
    val descripcion_mgp: String? = null,
    val descripcion_actividad: String
)

data class Solicitud(
    val id: Int,
    val conductor_id: Int,
    val tipo_destino: String,
    val vehiculo_id: Int?,
    val descripcion_mgp: String?,
    val descripcion_actividad: String,
    val estado: String,
    val conductor: Conductor? = null,
    val vehiculo: Vehiculo? = null,
    val orden: Orden? = null,
    val created_at: String? = null,
    val updated_at: String? = null
)

// Modelos para ordenes de carga de combustible
data class DetalleLineaRequest(
    val tipo_destino: String,
    val vehiculo_id: Int? = null,
    val descripcion_mgp: String? = null,
    val monto: Double
)

data class AsignarOrdenRequest(
    val solicitud_id: Int,
    val gasolinera_id: Int,
    val horas_vigencia: Int,
    val detalles: List<DetalleLineaRequest>
)

data class RehabilitarOrdenRequest(
    val horas_vigencia: Int
)

data class EscanearRequest(
    val codigo_qr: String
)

data class Evidencia(
    val id: Int,
    val tipo: String,
    val fecha_subida: String?,
    val url: String?
)

data class DetalleOrden(
    val id: Int,
    val orden_id: Int,
    val tipo_destino: String,
    val vehiculo_id: Int?,
    val descripcion_mgp: String?,
    val tipo_combustible: String,
    val monto: Double,
    val vehiculo: Vehiculo? = null,
    val evidencias: List<Evidencia> = emptyList()
)

data class Orden(
    val id: Int,
    val numero_orden: String,
    val solicitud_id: Int,
    val conductor_id: Int,
    val gasolinera_id: Int,
    val estado: String,
    val horas_vigencia: Int,
    val fecha_asignacion: String,
    val vence_asignacion: String,
    val fecha_apertura: String?,
    val vence_apertura: String?,
    val fecha_completada: String?,
    val justificacion: String? = null,
    val gasolinera: Gasolinera? = null,
    val conductor: Conductor? = null,
    val detalles: List<DetalleOrden>? = null
)

// Modelos para caja chica
data class CajaChicaRequest(
    val conductor_id: Int?,
    val monto: Double,
    val descripcion: String,
    val fecha_entrega: String
)

data class CajaChica(
    val id: Int,
    val conductor_id: Int?,
    val registrado_por: Int,
    val monto: Double,
    val descripcion: String,
    val fecha_entrega: String,
    val conductor: Conductor? = null
)

// Modelos para reportes de consumo
data class ReporteConductor(
    val conductor_id: Int,
    val nombre_conductor: String,
    val cantidad_ordenes: Int,
    val monto_total: Double
)

data class ReporteGasolinera(
    val gasolinera_id: Int,
    val nombre_gasolinera: String,
    val cantidad_ordenes: Int,
    val monto_total: Double
)

// Respuesta paginada generica enviada por Laravel
data class PaginaRespuesta<T>(
    val data: List<T>,
    val current_page: Int? = null,
    val last_page: Int? = null,
    val total: Int? = null
)

// Respuesta de mensaje simple para manejo de errores
data class MensajeResponse(val mensaje: String? = null, val message: String? = null) {
    val texto: String get() = mensaje ?: message ?: "Ocurrio un error inesperado."
}

// Modelos para verificación de contraseña de administrador
data class VerificarContrasenaRequest(
    val contrasena: String
)

data class VerificarContrasenaResponse(
    val permitido: Boolean,
    val usuario_id: Int? = null,
    val rol: String? = null,
    val mensaje: String? = null
)

data class ResumenReporte(
    val monto_total: Double, val cantidad_ordenes: Int, val promedio_orden: Double,
    val completadas_tarde: Int, val vencidas: Int, val cumplimiento_pct: Double
)
data class PuntoMes(val mes: String, val monto_total: Double, val cantidad_ordenes: Int)
data class PuntoCombustible(val tipo_combustible: String, val monto_total: Double, val cantidad_ordenes: Int)
data class PuntoRanking(val id: Int, val nombre: String, val monto_total: Double, val cantidad_ordenes: Int)
data class OrdenReporte(
    val orden_id: Int, val numero_orden: String, val fecha_completada: String, val estado: String,
    val gasolinera: String, val conductor: String, val destino: String,
    val tipo_combustible: String, val monto: Double
)
data class ConductorInfo(val id: Int, val nombre: String, val cargo: String?, val activo: Boolean)
data class GasolineraInfo(val id: Int, val nombre: String, val direccion: String?, val activo: Boolean)
data class ReporteResumenResp(
    val resumen: ResumenReporte, val por_mes: List<PuntoMes>, val por_combustible: List<PuntoCombustible>,
    val top_conductores: List<PuntoRanking>, val top_gasolineras: List<PuntoRanking>
)
data class ReporteConductorDetalleResp(
    val conductor: ConductorInfo, val resumen: ResumenReporte, val por_mes: List<PuntoMes>,
    val por_combustible: List<PuntoCombustible>, val por_gasolinera: List<PuntoRanking>,
    val ordenes: List<OrdenReporte>
)
data class ReporteGasolineraDetalleResp(
    val gasolinera: GasolineraInfo, val resumen: ResumenReporte, val por_mes: List<PuntoMes>,
    val por_combustible: List<PuntoCombustible>, val por_conductor: List<PuntoRanking>,
    val ordenes: List<OrdenReporte>
)
