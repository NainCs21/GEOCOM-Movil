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
