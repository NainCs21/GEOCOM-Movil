package com.example.apimovil.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apimovil.R
import com.example.apimovil.data.SessionManager
import com.example.apimovil.data.models.Conductor
import com.example.apimovil.databinding.FragmentOrdenesAdminBinding
import com.example.apimovil.ui.common.OrdenAdapter
import com.example.apimovil.ui.common.configurarNavegacionAdmin
import com.example.apimovil.ui.common.configurarNavegacionSupervisor
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class OrdenesAdminFragment : Fragment(R.layout.fragment_ordenes_admin) {

    private val model: AdminViewModel by viewModels()
    private var _binding: FragmentOrdenesAdminBinding? = null
    private val binding get() = _binding!!
    private val adapter = OrdenAdapter { orden ->
        val bundle = Bundle().apply { putInt("ordenId", orden.id) }
        findNavController().navigate(R.id.ordenDetalleAdminFragment, bundle)
    }

    private val estados = listOf("Todos los estados", "pendiente", "abierta", "completada", "vencida", "rehabilitada")
    private var estadoSeleccionado: String? = null
    private var conductorIdSeleccionado: Int? = null
    private var listaConductores: List<Conductor> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentOrdenesAdminBinding.bind(view)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter

        val bottomNav = binding.incluirNavInferior.root as BottomNavigationView
        if (SessionManager.esAdmin) {
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_admin)
            bottomNav.configurarNavegacionAdmin(findNavController(), R.id.ordenesAdminFragment)
        } else {
            bottomNav.menu.clear()
            bottomNav.inflateMenu(R.menu.bottom_nav_supervisor)
            bottomNav.configurarNavegacionSupervisor(findNavController(), R.id.ordenesAdminFragment)
        }

        // Spinner Estado
        binding.spinnerEstado.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, estados)
        )
        binding.spinnerEstado.setText(estados.first(), false)
        binding.spinnerEstado.setOnItemClickListener { _, _, position, _ ->
            val sel = estados[position]
            estadoSeleccionado = if (sel == "Todos los estados") null else sel
            model.cargarOrdenes(estadoSeleccionado, conductorIdSeleccionado)
        }

        // Spinner Conductor
        binding.spinnerConductor.setOnItemClickListener { _, _, position, _ ->
            if (position == 0) {
                conductorIdSeleccionado = null
            } else {
                val conductor = listaConductores.getOrNull(position - 1)
                conductorIdSeleccionado = conductor?.id
            }
            model.cargarOrdenes(estadoSeleccionado, conductorIdSeleccionado)
        }

        binding.swipeRefresh.setOnRefreshListener {
            model.cargarOrdenes(estadoSeleccionado, conductorIdSeleccionado)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.ordenes.collect { estado ->
                    binding.swipeRefresh.isRefreshing = estado.loading
                    binding.textVacio.isVisible = !estado.loading && estado.error == null && estado.data.isNullOrEmpty()
                    binding.textError.isVisible = estado.error != null
                    binding.textError.text = estado.error
                    adapter.submitList(estado.data.orEmpty())
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.conductores.collect { estado ->
                    listaConductores = estado.data.orEmpty()
                    val nombresConductores = mutableListOf(getString(R.string.todos_los_conductores))
                    nombresConductores.addAll(listaConductores.map { it.usuario?.name ?: "Conductor #${it.id}" })
                    binding.spinnerConductor.setAdapter(
                        ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, nombresConductores)
                    )
                    if (binding.spinnerConductor.text.isNullOrBlank()) {
                        binding.spinnerConductor.setText(nombresConductores.first(), false)
                    }
                }
            }
        }

        model.cargarConductores()
        model.cargarOrdenes()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
