package com.example.apimovil.ui.conductor

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.apimovil.R
import com.example.apimovil.data.SessionManager
import com.example.apimovil.data.notificaciones.NotificacionesForegroundService
import com.example.apimovil.data.notificaciones.cancelarSincronizacionNotificaciones
import com.example.apimovil.databinding.FragmentPerfilConductorBinding
import com.example.apimovil.ui.common.configurarNavegacionConductor
import kotlinx.coroutines.launch

class PerfilConductorFragment : Fragment(R.layout.fragment_perfil_conductor) {

    private val model: ConductorViewModel by viewModels()
    private var _binding: FragmentPerfilConductorBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentPerfilConductorBinding.bind(view)
        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }
        binding.textNombre.text = SessionManager.nombre ?: ""
        binding.textCorreo.text = SessionManager.correo ?: ""

        binding.btnCerrarSesion.setOnClickListener {
            binding.btnCerrarSesion.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                SessionManager.cerrarSesionRemotaYLocal()
                val appContext = requireContext().applicationContext
                NotificacionesForegroundService.detener(appContext)
                cancelarSincronizacionNotificaciones(appContext)
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()
                findNavController().navigate(R.id.loginFragment, null, navOptions)
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
