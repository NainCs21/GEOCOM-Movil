package com.example.apimovil.ui.admin

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.apimovil.R
import com.example.apimovil.data.SessionManager
import com.example.apimovil.data.notificaciones.NotificacionesStore
import com.example.apimovil.databinding.FragmentMasBinding
import com.example.apimovil.ui.common.configurarNavegacionAdmin
import com.example.apimovil.ui.common.configurarNavegacionSupervisor
import com.example.apimovil.ui.common.vincularBadgeAvisos
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import kotlinx.coroutines.launch

@ExperimentalBadgeUtils
@Suppress("RestrictedApi")
class MasFragment : Fragment(R.layout.fragment_mas) {

    private var _binding: FragmentMasBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentMasBinding.bind(view)

        val bottomNav = binding.incluirNavInferior.root
        if (SessionManager.esAdmin) {
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_admin)
            bottomNav.configurarNavegacionAdmin(findNavController(), R.id.masFragment)
            bottomNav.vincularBadgeAvisos(viewLifecycleOwner, R.id.nav_mas)
        } else {
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_supervisor)
            bottomNav.configurarNavegacionSupervisor(findNavController(), R.id.masFragment)
            bottomNav.vincularBadgeAvisos(viewLifecycleOwner, R.id.nav_avisos)
        }

        val badgeMas = BadgeDrawable.create(requireContext())
        BadgeUtils.attachBadgeDrawable(badgeMas, binding.opcionNotificaciones)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                NotificacionesStore.noLeidas.collect { count ->
                    badgeMas.isVisible = count > 0
                    badgeMas.number = count
                }
            }
        }

        binding.opcionNotificaciones.setOnClickListener { findNavController().navigate(R.id.notificacionesFragment) }
        binding.opcionCatalogos.isVisible = SessionManager.esAdmin
        binding.opcionCatalogos.setOnClickListener { findNavController().navigate(R.id.catalogosFragment) }
        binding.opcionPerfil.setOnClickListener { findNavController().navigate(R.id.perfilAdminFragment) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
