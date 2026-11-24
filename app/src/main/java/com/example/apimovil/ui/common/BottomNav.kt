package com.example.apimovil.ui.common

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import com.example.apimovil.R
import com.example.apimovil.data.notificaciones.NotificacionesStore
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Conecta la barra inferior de Administrador/Supervisor con el NavController,
 * marcando como seleccionado el destino en el que ya estamos (para que no
 * se recargue la misma pantalla al tocar su propio ícono) y navegando con
 * popUpTo al destino raíz de esa pestaña para no apilar pantallas repetidas.
 */
fun BottomNavigationView.configurarNavegacionAdmin(nav: NavController, destinoActual: Int) {
    isSaveEnabled = false
    menu.findItem(destinoActualAItemAdmin(destinoActual))?.isChecked = true

    setOnItemSelectedListener { item ->
        val destino = when (item.itemId) {
            R.id.nav_inicio -> R.id.homeAdminFragment
            R.id.nav_solicitudes -> R.id.solicitudesPendientesFragment
            R.id.nav_ordenes -> R.id.ordenesAdminFragment
            R.id.nav_reportes -> R.id.reportesFragment
            R.id.nav_mas -> R.id.masFragment
            else -> null
        }
        if (destino != null && destino != destinoActual) {
            val opciones = NavOptions.Builder()
                .setPopUpTo(R.id.homeAdminFragment, false)
                .build()
            nav.navigate(destino, null, opciones)
        }
        true
    }
}

private fun destinoActualAItemAdmin(destino: Int): Int = when (destino) {
    R.id.homeAdminFragment -> R.id.nav_inicio
    R.id.solicitudesPendientesFragment -> R.id.nav_solicitudes
    R.id.ordenesAdminFragment -> R.id.nav_ordenes
    R.id.reportesFragment -> R.id.nav_reportes
    else -> R.id.nav_mas
}

/** Igual que la de arriba, para la barra de Supervisor (destinos distintos). */
fun BottomNavigationView.configurarNavegacionSupervisor(nav: NavController, destinoActual: Int) {
    isSaveEnabled = false
    menu.findItem(destinoActualAItemSupervisor(destinoActual))?.isChecked = true

    setOnItemSelectedListener { item ->
        val destino = when (item.itemId) {
            R.id.nav_inicio -> R.id.homeAdminFragment
            R.id.nav_ordenes -> R.id.ordenesAdminFragment
            R.id.nav_avisos -> R.id.notificacionesFragment
            R.id.nav_reportes -> R.id.reportesFragment
            R.id.nav_perfil -> R.id.perfilAdminFragment
            else -> null
        }
        if (destino != null && destino != destinoActual) {
            val opciones = NavOptions.Builder()
                .setPopUpTo(R.id.homeAdminFragment, false)
                .build()
            nav.navigate(destino, null, opciones)
        }
        true
    }
}

private fun destinoActualAItemSupervisor(destino: Int): Int = when (destino) {
    R.id.homeAdminFragment -> R.id.nav_inicio
    R.id.ordenesAdminFragment -> R.id.nav_ordenes
    R.id.notificacionesFragment -> R.id.nav_avisos
    R.id.reportesFragment -> R.id.nav_reportes
    else -> R.id.nav_perfil
}

fun BottomNavigationView.configurarNavegacionConductor(
    nav: NavController,
    destinoActual: Int
) {
    isSaveEnabled = false
    menu.findItem(destinoActualAItemConductor(destinoActual))?.isChecked = true

    setOnItemSelectedListener { item ->
        when (item.itemId) {
            R.id.nav_conductor_inicio -> {
                if (destinoActual != R.id.homeConductorFragment) {
                    val opciones = NavOptions.Builder().setPopUpTo(R.id.homeConductorFragment, false).build()
                    nav.navigate(R.id.homeConductorFragment, null, opciones)
                }
                true
            }
            R.id.nav_conductor_historial -> {
                if (destinoActual != R.id.historialFragment) {
                    val opciones = NavOptions.Builder().setPopUpTo(R.id.homeConductorFragment, false).build()
                    nav.navigate(R.id.historialFragment, null, opciones)
                }
                true
            }
            R.id.nav_conductor_perfil -> {
                if (destinoActual != R.id.perfilConductorFragment) {
                    val opciones = NavOptions.Builder().setPopUpTo(R.id.homeConductorFragment, false).build()
                    nav.navigate(R.id.perfilConductorFragment, null, opciones)
                }
                true
            }
            else -> false
        }
    }
}

private fun destinoActualAItemConductor(destino: Int): Int = when (destino) {
    R.id.homeConductorFragment -> R.id.nav_conductor_inicio
    R.id.historialFragment -> R.id.nav_conductor_historial
    R.id.perfilConductorFragment -> R.id.nav_conductor_perfil
    else -> R.id.nav_conductor_inicio
}

fun BottomNavigationView.vincularBadgeAvisos(owner: LifecycleOwner, itemId: Int) {
    owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            NotificacionesStore.noLeidas.collect { count ->
                try {
                    if (menu.findItem(itemId) != null) {
                        val badge = getOrCreateBadge(itemId)
                        badge.isVisible = count > 0
                        badge.number = count
                    }
                } catch (_: Exception) {}
            }
        }
    }
}

fun BottomNavigationView.vincularBadgeSolicitudes(owner: LifecycleOwner, itemId: Int, countFlow: kotlinx.coroutines.flow.Flow<Int>) {
    owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            countFlow.collect { count ->
                try {
                    if (menu.findItem(itemId) != null) {
                        val badge = getOrCreateBadge(itemId)
                        badge.isVisible = count > 0
                        badge.number = count
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
