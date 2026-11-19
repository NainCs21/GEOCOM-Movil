package com.example.apimovil

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.apimovil.data.ApiClient
import com.example.apimovil.data.SessionManager
import com.example.apimovil.data.SesionEventos
import com.example.apimovil.data.notificaciones.NotificacionesForegroundService
import com.example.apimovil.data.notificaciones.NotificacionesRepository
import com.example.apimovil.data.notificaciones.NotificacionesStore
import com.example.apimovil.data.notificaciones.cancelarSincronizacionNotificaciones
import com.example.apimovil.data.notificaciones.programarSincronizacionNotificaciones
import com.example.apimovil.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        SessionManager.init(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController: NavController = navHost.navController

        val esArranqueEnFrio = GeoComApp.arranqueEnFrio
        GeoComApp.arranqueEnFrio = false

        if (SessionManager.haySesion) {
            if (esArranqueEnFrio) {
                val destino = if (SessionManager.esConductor) R.id.homeConductorFragment else R.id.homeAdminFragment
                val graph = navController.navInflater.inflate(R.navigation.nav_graph)
                graph.setStartDestination(destino)
                navController.graph = graph
            }

            // Cold start call to /yo
            lifecycleScope.launch {
                try {
                    val usuario = ApiClient.api.yo()
                    val rolAnterior = SessionManager.rol
                    SessionManager.actualizarUsuario(usuario.nombre, usuario.correo, usuario.rol)
                    if (usuario.rol != rolAnterior) {
                        if (SessionManager.puedeConsultar) {
                            NotificacionesForegroundService.iniciar(this@MainActivity)
                            programarSincronizacionNotificaciones(this@MainActivity)
                        } else {
                            NotificacionesForegroundService.detener(this@MainActivity)
                            cancelarSincronizacionNotificaciones(this@MainActivity)
                        }
                        if (esArranqueEnFrio) {
                            val nuevoDestino = if (SessionManager.esConductor) R.id.homeConductorFragment else R.id.homeAdminFragment
                            val graph = navController.navInflater.inflate(R.navigation.nav_graph)
                            graph.setStartDestination(nuevoDestino)
                            navController.graph = graph
                        }
                    }
                } catch (_: IOException) {
                    // Offline mode, maintain session
                } catch (_: Exception) {
                    // Other errors (401 handled by interceptor)
                }
            }

            if (SessionManager.puedeConsultar) {
                NotificacionesForegroundService.iniciar(this)
                programarSincronizacionNotificaciones(this)
                lifecycleScope.launch {
                    NotificacionesRepository.sincronizar(this@MainActivity)
                }
            }
        }

        verificarYPedirPermisoNotificaciones()
        handleNotificationIntent(intent, navController)

        // Observar eventos globales de sesion y chequeo periodico de notificaciones
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    while (true) {
                        if (SessionManager.haySesion && SessionManager.puedeConsultar) {
                            NotificacionesRepository.sincronizar(this@MainActivity)
                        }
                        delay(10_000L) // Chequeo de notificaciones cada 10 segundos en primer plano
                    }
                }
                launch {
                    SesionEventos.sesionExpirada.collect { mensaje ->
                        SessionManager.guardarMensajeSalida(mensaje)
                        NotificacionesForegroundService.detener(this@MainActivity)
                        cancelarSincronizacionNotificaciones(this@MainActivity)
                        val currentDest = navController.currentDestination?.id
                        if (currentDest != null && currentDest != R.id.loginFragment) {
                            val navOptions = NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph, true)
                                .build()
                            navController.navigate(R.id.loginFragment, null, navOptions)
                        }
                        SesionEventos.reiniciarGuardia()
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val navHost = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        handleNotificationIntent(intent, navHost.navController)
    }

    private fun handleNotificationIntent(intent: Intent, navController: NavController) {
        val notifId = intent.getStringExtra("notif_id")
        val destino = intent.getStringExtra("notif_destino")
        val ordenId = intent.getIntExtra("notif_orden_id", -1)

        if (!notifId.isNullOrBlank()) {
            NotificacionesStore.marcarLeida(notifId)
        }

        if (!destino.isNullOrBlank()) {
            binding.root.post {
                executeNavigation(navController, destino, ordenId)
            }
            intent.removeExtra("notif_id")
            intent.removeExtra("notif_destino")
            intent.removeExtra("notif_orden_id")
        }
    }

    private fun executeNavigation(navController: NavController, destino: String, ordenId: Int) {
        if (!SessionManager.haySesion || !SessionManager.puedeConsultar) return
        try {
            when (destino) {
                "orden" -> {
                    if (ordenId != -1) {
                        val bundle = Bundle().apply { putInt("ordenId", ordenId) }
                        navController.navigate(R.id.ordenDetalleAdminFragment, bundle)
                    }
                }
                "solicitudes" -> {
                    navController.navigate(R.id.solicitudesPendientesFragment)
                }
                "notificaciones" -> {
                    navController.navigate(R.id.notificacionesFragment)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        verificarYPedirPermisoNotificaciones()
        if (SessionManager.haySesion && SessionManager.puedeConsultar) {
            NotificacionesForegroundService.iniciar(this)
            programarSincronizacionNotificaciones(this)
            lifecycleScope.launch {
                NotificacionesRepository.sincronizar(this@MainActivity)
            }
        }
    }

    private fun verificarYPedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (SessionManager.haySesion && SessionManager.puedeConsultar) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        1001,
                    )
                }
            }
        }
    }
}
