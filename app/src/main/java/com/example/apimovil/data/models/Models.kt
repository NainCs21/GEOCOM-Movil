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
    val activo: Boolean,
    val rendimiento_km_litro: Double? = null
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
    val evidencias: List<Evidencia> = emptyList(),
    val combustible_precio: String? = null,
    val precio_galon: Double? = null,
    val precio_vigente_desde: String? = null,
    val galones_estimados: Double? = null,
    val km_estimados: Double? = null
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
data class MensajeResponse(
    val mensaje: String? = null,
    val message: String? = null,
    val errors: Map<String, List<String>>? = null
) {
    val texto: String get() = mensaje ?: message ?: "Ocurrió un error inesperado."
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
    val completadas_tarde: Int, val vencidas: Int, val cumplimiento_pct: Double,
    val galones_total: Double = 0.0, val km_total: Double = 0.0, val precio_promedio_galon: Double? = null
)
data class PuntoMes(
    val mes: String, val monto_total: Double, val cantidad_ordenes: Int,
    val galones_total: Double = 0.0, val km_total: Double = 0.0, val precio_promedio_galon: Double? = null
)
data class PuntoCombustible(
    val tipo_combustible: String, val monto_total: Double, val cantidad_ordenes: Int,
    val galones_total: Double = 0.0, val km_total: Double = 0.0, val precio_promedio_galon: Double? = null
)
data class PuntoRanking(
    val id: Int, val nombre: String, val monto_total: Double, val cantidad_ordenes: Int,
    val galones_total: Double = 0.0, val km_total: Double = 0.0, val precio_promedio_galon: Double? = null
)
data class OrdenReporte(
    val orden_id: Int, val numero_orden: String, val fecha_completada: String, val estado: String,
    val gasolinera: String, val conductor: String, val destino: String,
    val tipo_combustible: String, val monto: Double,
    val precio_galon: Double? = null, val galones_estimados: Double? = null,
    val km_estimados: Double? = null
)
data class ConductorInfo(val id: Int, val nombre: String, val cargo: String?, val activo: Boolean)
data class GasolineraInfo(val id: Int, val nombre: String, val direccion: String?, val activo: Boolean)
data class ReporteResumenResp(
    val resumen: ResumenReporte, val por_mes: List<PuntoMes>, val por_combustible: List<PuntoCombustible>,
    val top_conductores: List<PuntoRanking>, val top_gasolineras: List<PuntoRanking>
)
data class PuntoVehiculo(
    val id: Int, val placa: String, val descripcion: String, val rendimiento_km_litro: Double?,
    val monto_total: Double, val cantidad_ordenes: Int,
    val galones_total: Double = 0.0, val km_total: Double = 0.0, val precio_promedio_galon: Double? = null
)
data class ReporteConductorDetalleResp(
    val conductor: ConductorInfo, val resumen: ResumenReporte, val por_mes: List<PuntoMes>,
    val por_combustible: List<PuntoCombustible>, val por_gasolinera: List<PuntoRanking>,
    val ordenes: List<OrdenReporte>,
    val por_vehiculo: List<PuntoVehiculo> = emptyList()
)
data class ReporteGasolineraDetalleResp(
    val gasolinera: GasolineraInfo, val resumen: ResumenReporte, val por_mes: List<PuntoMes>,
    val por_combustible: List<PuntoCombustible>, val por_conductor: List<PuntoRanking>,
    val ordenes: List<OrdenReporte>
)
data class PrecioCombustible(
    val id: Int, val combustible: String, val ciudad: String, val precio: Double,
    val variacion: Double, val vigente_desde: String
)
data class EstimacionRequest(
    val monto: Double, val vehiculo_id: Int? = null, val tipo_combustible: String? = null
)
data class EstimacionCombustible(
    val monto_autorizado: Double, val combustible: String, val ciudad: String,
    val precio_por_galon: Double, val vigente_desde: String?,
    val galones_aproximados: Double, val litros_aproximados: Double,
    val rendimiento_km_litro: Double?, val kilometros_aproximados: Double?
)
data class HistorialPrecios(val ciudad: String, val semanas: List<Map<String, Any>>)
