package com.example.apimovil.ui.admin

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.apimovil.R
import com.example.apimovil.data.SessionManager
import com.example.apimovil.databinding.FragmentHomeAdminBinding
import com.example.apimovil.ui.common.configurarNavegacionAdmin
import com.example.apimovil.ui.common.configurarNavegacionSupervisor
import com.example.apimovil.ui.common.formatoLegible
import com.example.apimovil.ui.common.parseFechaIso
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeAdminFragment : Fragment(R.layout.fragment_home_admin) {

    private val model: AdminViewModel by viewModels()
    private var _binding: FragmentHomeAdminBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentHomeAdminBinding.bind(view)

        binding.textRolBarra.text = if (SessionManager.esAdmin)
            getString(R.string.rol_administrador) else getString(R.string.rol_supervisor)
        binding.textSaludo.text = getString(R.string.buen_dia_usuario, SessionManager.nombre ?: "")
        binding.textFecha.text = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("es", "HN"))
            .format(Date()).uppercase()

        // La barra inferior es distinta para admin (5 pestañas de gestión)
        // y supervisor (5 pestañas de solo consulta).
        val bottomNav = binding.incluirNavInferior.root as BottomNavigationView
        if (SessionManager.esAdmin) {
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_admin)
            bottomNav.configurarNavegacionAdmin(findNavController(), R.id.homeAdminFragment)
        } else {
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_supervisor)
            bottomNav.configurarNavegacionSupervisor(findNavController(), R.id.homeAdminFragment)
        }

        binding.btnNotificaciones.setOnClickListener {
            findNavController().navigate(R.id.notificacionesFragment)
        }
        binding.btnAsignarMontos.setOnClickListener {
            findNavController().navigate(R.id.solicitudesPendientesFragment)
        }
        binding.btnCatalogosRapido.setOnClickListener {
            findNavController().navigate(R.id.catalogosFragment)
        }
        binding.btnCajaChicaRapido.setOnClickListener {
            findNavController().navigate(R.id.cajaChicaFragment)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    model.solicitudesPendientes.collect { estado ->
                        binding.textConteoPendientes.text = (estado.data?.size ?: 0).toString()
                    }
                }
                launch {
                    model.ordenes.collect { estado ->
                        val ordenes = estado.data.orEmpty()
                        binding.textAbiertas.text = ordenes.count { it.estado == "abierta" }.toString()
                        binding.textCompletadas.text = ordenes.count { it.estado == "completada" }.toString()
                        binding.textVencidas.text = ordenes.count { it.estado == "vencida" }.toString()

                        val proxima = ordenes
                            .filter { it.estado == "abierta" && it.vence_apertura != null }
                            .minByOrNull { parseFechaIso(it.vence_apertura!!).time }

                        if (proxima != null) {
                            binding.contenedorProximaAVencer.isVisible = true
                            binding.textOrdenProxima.text = proxima.numero_orden
                            val vence = parseFechaIso(proxima.vence_apertura!!).time
                            val minutosRestantes = ((vence - System.currentTimeMillis()) / 60000).coerceAtLeast(0)
                            binding.textGasolineraYVence.text = getString(
                                R.string.vence_a_las,
                                proxima.gasolinera?.nombre ?: "",
                                formatoLegible(proxima.vence_apertura!!)
                            )
                            binding.textMinutosRestantes.text = getString(R.string.quedan_n_min, minutosRestantes.toInt())
                            binding.btnVerOrden.setOnClickListener {
                                val bundle = Bundle().apply { putInt("ordenId", proxima.id) }
                                findNavController().navigate(R.id.ordenDetalleAdminFragment, bundle)
                            }
                        } else {
                            binding.contenedorProximaAVencer.isVisible = false
                        }
                    }
                }
            }
        }

        model.cargarSolicitudesPendientes()
        model.cargarOrdenes()
    }

    override fun onResume() {
        super.onResume()
        model.cargarSolicitudesPendientes()
        model.cargarOrdenes()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
