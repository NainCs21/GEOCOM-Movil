package com.example.apimovil.data

import com.example.apimovil.data.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

// Interfaz con los metodos de la API backend
interface ApiService {

    // Autenticacion
    @POST("login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("logout")
    suspend fun logout()

    @GET("yo")
    suspend fun yo(): UsuarioInfo

    // Solicitudes
    @POST("solicitudes")
    suspend fun crearSolicitud(@Body body: SolicitudRequest): Solicitud

    @GET("solicitudes")
    suspend fun listarSolicitudes(): PaginaRespuesta<Solicitud>

    @GET("solicitudes/{id}")
    suspend fun verSolicitud(@Path("id") id: Int): Solicitud

    @POST("solicitudes/{id}/rechazar")
    suspend fun rechazarSolicitud(@Path("id") id: Int): Solicitud

    // Ordenes
    @GET("ordenes")
    suspend fun listarOrdenes(
        @Query("estado") estado: String? = null,

            @Query("conductor_id") conductorId: Int? = null,
        @Query("gasolinera_id") gasolineraId: Int? = null,
    ): PaginaRespuesta<Orden>

    @GET("ordenes/{id}")
    suspend fun verOrden(@Path("id") id: Int): Orden

    @POST("ordenes")
    suspend fun crearOrden(@Body body: AsignarOrdenRequest): Orden

    @POST("ordenes/escanear")
    suspend fun escanearOrden(@Body body: EscanearRequest): Orden

    @POST("ordenes/{id}/rehabilitar")
    suspend fun rehabilitarOrden(@Path("id") id: Int): Orden

    @Multipart
    @POST("detalle-ordenes/{id}/evidencia")
    suspend fun subirEvidencia(
        @Path("id") detalleId: Int,
        @Part("tipo") tipo: RequestBody?,
        @Part("justificacion") justificacion: RequestBody?,
        @Part foto: MultipartBody.Part,
    ): Orden

    // Catalogos
    @GET("conductores")
    suspend fun listarConductores(): PaginaRespuesta<Conductor>

    @GET("conductores/{id}")
    suspend fun verConductor(@Path("id") id: Int): Conductor

    @GET("vehiculos")
    suspend fun listarVehiculos(): PaginaRespuesta<Vehiculo>

    @GET("vehiculos/{id}")
    suspend fun verVehiculo(@Path("id") id: Int): Vehiculo

    @GET("gasolineras")
    suspend fun listarGasolineras(): PaginaRespuesta<Gasolinera>

    @GET("gasolineras/{id}")
    suspend fun verGasolinera(@Path("id") id: Int): Gasolinera

    @POST("conductores")
    suspend fun crearConductor(@Body body: Map<String, @JvmSuppressWildcards Any?>): Conductor

    @PUT("conductores/{id}")
    suspend fun editarConductor(
        @Path("id") id: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Conductor

    @DELETE("conductores/{id}")
    suspend fun desactivarConductor(@Path("id") id: Int)

    @POST("vehiculos")
    suspend fun crearVehiculo(@Body body: Map<String, @JvmSuppressWildcards Any?>): Vehiculo

    @PUT("vehiculos/{id}")
    suspend fun editarVehiculo(
        @Path("id") id: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Vehiculo

    @DELETE("vehiculos/{id}")
    suspend fun desactivarVehiculo(@Path("id") id: Int)

    @POST("gasolineras")
    suspend fun crearGasolinera(@Body body: Map<String, @JvmSuppressWildcards Any?>): Gasolinera

    @PUT("gasolineras/{id}")
    suspend fun editarGasolinera(
        @Path("id") id: Int,
        @Body body: Map<String, @JvmSuppressWildcards Any?>,
    ): Gasolinera

    @DELETE("gasolineras/{id}")
    suspend fun desactivarGasolinera(@Path("id") id: Int)

    // Caja chica
    @GET("caja-chica")
    suspend fun listarCajaChica(): PaginaRespuesta<CajaChica>

    @GET("caja-chica/{id}")
    suspend fun verCajaChica(@Path("id") id: Int): CajaChica

    @POST("caja-chica")
    suspend fun registrarCajaChica(@Body body: CajaChicaRequest): CajaChica

    // Reportes
    @GET("reportes/por-conductor")
    suspend fun reportePorConductor(
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null,
    ): List<ReporteConductor>

    @GET("reportes/por-gasolinera")
    suspend fun reportePorGasolinera(
        @Query("desde") desde: String? = null,
        @Query("hasta") hasta: String? = null,
    ): List<ReporteGasolinera>
}
