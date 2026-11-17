package com.example.apimovil.ui.admin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.apimovil.R
import com.example.apimovil.data.SessionManager
import com.example.apimovil.databinding.FragmentMasBinding
import com.example.apimovil.ui.common.configurarNavegacionAdmin
import com.example.apimovil.ui.common.configurarNavegacionSupervisor
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Quinta pestaña de Admin ("Más"): agrupa los destinos que no caben en la
 * barra de 5 íconos (notificaciones, reportes, caja chica, perfil).
 */
class MasFragment : Fragment(R.layout.fragment_mas) {

    private var _binding: FragmentMasBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMasBinding.bind(view)

        val bottomNav = binding.incluirNavInferior.root as BottomNavigationView
        if (SessionManager.esAdmin) {
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_admin)
            bottomNav.configurarNavegacionAdmin(findNavController(), R.id.masFragment)
        } else {
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_supervisor)
            bottomNav.configurarNavegacionSupervisor(findNavController(), R.id.masFragment)
        }

        binding.opcionNotificaciones.setOnClickListener { findNavController().navigate(R.id.notificacionesFragment) }
        binding.opcionReportes.setOnClickListener { findNavController().navigate(R.id.reportesFragment) }
        binding.opcionCajaChica.setOnClickListener { findNavController().navigate(R.id.cajaChicaFragment) }
        binding.opcionPerfil.setOnClickListener { findNavController().navigate(R.id.perfilAdminFragment) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
