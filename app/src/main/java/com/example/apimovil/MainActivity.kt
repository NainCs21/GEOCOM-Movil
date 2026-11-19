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
import com.example.apimovil.data.SessionManager
import com.example.apimovil.data.SesionEventos
import com.example.apimovil.data.notificaciones.NotificacionesRepository
import com.example.apimovil.data.notificaciones.NotificacionesStore
import com.example.apimovil.data.notificaciones.cancelarSincronizacionNotificaciones
import com.example.apimovil.data.notificaciones.programarSincronizacionNotificaciones
import com.example.apimovil.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

        // Si ya hay sesión guardada, nos saltamos el login directo al panel que corresponda.
        if (SessionManager.haySesion) {
            val destino = if (SessionManager.esConductor) R.id.homeConductorFragment else R.id.homeAdminFragment
            val graph = navController.navInflater.inflate(R.navigation.nav_graph)
            graph.setStartDestination(destino)
            navController.graph = graph

            if (SessionManager.puedeConsultar) {
                programarSincronizacionNotificaciones(this)
                lifecycleScope.launch {
                    NotificacionesRepository.sincronizar(this@MainActivity)
                }
            }
        } else {
            cancelarSincronizacionNotificaciones(this)
        }

        // POST_NOTIFICATIONS permission for API 33+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (SessionManager.haySesion && SessionManager.puedeConsultar) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        1001
                    )
                }
            }
        }

        handleNotificationIntent(intent, navController)

        // Observar eventos globales de sesion (expirada por 401 o inactividad) y chequeo periodico
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    SesionEventos.sesionExpirada.collect { mensaje ->
                        SessionManager.guardarMensajeSalida(mensaje)
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
                launch {
                    while (true) {
                        delay(30_000L) // Chequeo periodico cada 30 segundos
                        verificarInactividadYSalir()
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
            }
        } catch (_: Exception) {}
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        if (SessionManager.haySesion) {
            SessionManager.actualizarUltimaActividad()
        }
    }

    override fun onResume() {
        super.onResume()
        verificarInactividadYSalir()
    }

    private fun verificarInactividadYSalir() {
        if (SessionManager.haySesion && SessionManager.haExpiradoInactividad()) {
            lifecycleScope.launch {
                cancelarSincronizacionNotificaciones(this@MainActivity)
                SessionManager.cerrarSesionRemotaYLocal()
                SesionEventos.notificarSesionExpirada("Tu sesión se cerró por inactividad.")
            }
        }
    }
}
