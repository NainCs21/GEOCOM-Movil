package com.example.apimovil

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.example.apimovil.data.SessionManager
import com.example.apimovil.data.SesionEventos
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
        }

        // Observar eventos globales de sesion (expirada por 401 o inactividad) y chequeo periodico
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    SesionEventos.sesionExpirada.collect { mensaje ->
                        SessionManager.guardarMensajeSalida(mensaje)
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
                SessionManager.cerrarSesionRemotaYLocal()
                SesionEventos.notificarSesionExpirada("Tu sesión se cerró por inactividad.")
            }
        }
    }
}
