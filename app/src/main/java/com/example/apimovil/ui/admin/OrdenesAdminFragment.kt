package com.example.apimovil.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
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
import com.example.apimovil.data.models.Gasolinera
import com.example.apimovil.databinding.BottomSheetFiltrosOrdenesBinding
import com.example.apimovil.databinding.FragmentOrdenesAdminBinding
import com.example.apimovil.ui.common.ElementoFiltro
import com.example.apimovil.ui.common.FiltroAdapter
import com.example.apimovil.ui.common.OrdenAdapter
import com.example.apimovil.ui.common.configurarNavegacionAdmin
import com.example.apimovil.ui.common.configurarNavegacionSupervisor
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class OpcionEstado(val etiqueta: String, val valor: String?)

class OrdenesAdminFragment : Fragment(R.layout.fragment_ordenes_admin) {

    private val model: AdminViewModel by viewModels()
    private var _binding: FragmentOrdenesAdminBinding? = null
    private val binding get() = _binding!!
    private val adapter = OrdenAdapter(mostrarConductor = true) { orden ->
        val bundle = Bundle().apply { putInt("ordenId", orden.id) }
        findNavController().navigate(R.id.ordenDetalleAdminFragment, bundle)
    }

    private val opcionesEstado by lazy {
        listOf(
            OpcionEstado(getString(R.string.todos_los_estados), null),
            OpcionEstado(getString(R.string.filtro_pendientes), "pendiente"),
            OpcionEstado(getString(R.string.filtro_abiertas), "abierta,abierta_tarde"),
            OpcionEstado(getString(R.string.filtro_completadas), "completada,completada_tarde"),
            OpcionEstado(getString(R.string.filtro_vencidas), "vencida"),
            OpcionEstado(getString(R.string.filtro_abierta_tarde), "abierta_tarde"),
            OpcionEstado(getString(R.string.filtro_completada_tarde), "completada_tarde"),
        )
    }

    private var estadoSeleccionado: String? = null
    private var conductorIdSeleccionado: Int? = null
    private var gasolineraIdSeleccionada: Int? = null
    private var desdeSeleccionado: String? = null
    private var hastaSeleccionado: String? = null

    private var listaConductores: List<Conductor> = emptyList()
    private var listaGasolineras: List<Gasolinera> = emptyList()

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

        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }
        binding.btnFiltros.setOnClickListener { mostrarBottomSheetFiltros() }

        val estadoArg = arguments?.getString("filtroEstado")
        if (estadoArg != null) {
            estadoSeleccionado = estadoArg
            aplicarFiltros()
        }

        binding.swipeRefresh.setOnRefreshListener {
            aplicarFiltros()
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
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.gasolineras.collect { estado ->
                    listaGasolineras = estado.data.orEmpty().filter { it.activo }
                }
            }
        }

        model.cargarConductores()
        model.cargarGasolineras()
        aplicarFiltros()
    }

    private fun aplicarFiltros() {
        actualizarChips()
        model.cargarOrdenes(
            estado = estadoSeleccionado,
            conductorId = conductorIdSeleccionado,
            gasolineraId = gasolineraIdSeleccionada,
            desde = desdeSeleccionado,
            hasta = hastaSeleccionado
        )
    }

    private fun actualizarChips() {
        binding.chipGroupFiltros.removeAllViews()

        var hayFiltro = false

        // 1. Estado
        estadoSeleccionado?.let { valApi ->
            hayFiltro = true
            val opcion = opcionesEstado.find { it.valor == valApi }
            val etiqueta = opcion?.etiqueta ?: valApi
            val chip = crearChip(etiqueta) {
                estadoSeleccionado = null
                aplicarFiltros()
            }
            binding.chipGroupFiltros.addView(chip)
        }

        // 2. Conductor
        conductorIdSeleccionado?.let { id ->
            hayFiltro = true
            val nombre = listaConductores.find { it.id == id }?.usuario?.name ?: "Conductor #$id"
            val chip = crearChip(nombre) {
                conductorIdSeleccionado = null
                aplicarFiltros()
            }
            binding.chipGroupFiltros.addView(chip)
        }

        // 3. Gasolinera
        gasolineraIdSeleccionada?.let { id ->
            hayFiltro = true
            val nombre = listaGasolineras.find { it.id == id }?.nombre ?: "Gasolinera #$id"
            val chip = crearChip(nombre) {
                gasolineraIdSeleccionada = null
                aplicarFiltros()
            }
            binding.chipGroupFiltros.addView(chip)
        }

        // 4. Fechas
        if (desdeSeleccionado != null && hastaSeleccionado != null) {
            hayFiltro = true
            val textoRango = formatearRangoChip(desdeSeleccionado!!, hastaSeleccionado!!)
            val chip = crearChip(textoRango) {
                desdeSeleccionado = null
                hastaSeleccionado = null
                aplicarFiltros()
            }
            binding.chipGroupFiltros.addView(chip)
        }

        binding.scrollChips.isVisible = hayFiltro
    }

    private fun crearChip(texto: String, onClose: () -> Unit): Chip {
        return Chip(requireContext()).apply {
            this.text = texto
            isCloseIconVisible = true
            textSize = 12f
            setChipBackgroundColorResource(R.color.marca_claro)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.marca_oscuro))
            setCloseIconTintResource(R.color.marca_oscuro)
            setOnCloseIconClickListener { onClose() }
        }
    }

    private fun formatearRangoChip(desdeIso: String, hastaIso: String): String {
        return try {
            val sdfApi = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val sdfChip = SimpleDateFormat("dd/MM", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val d1 = sdfApi.parse(desdeIso)
            val d2 = sdfApi.parse(hastaIso)
            if (d1 != null && d2 != null) "${sdfChip.format(d1)} - ${sdfChip.format(d2)}" else "$desdeIso - $hastaIso"
        } catch (_: Exception) {
            "$desdeIso - $hastaIso"
        }
    }

    private fun mostrarBottomSheetFiltros() {
        val sheetBinding = BottomSheetFiltrosOrdenesBinding.inflate(LayoutInflater.from(requireContext()))
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(sheetBinding.root)
        @Suppress("DEPRECATION")
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        var tempEstado = estadoSeleccionado
        var tempConductorId = conductorIdSeleccionado
        var tempGasolineraId = gasolineraIdSeleccionada
        var tempDesde = desdeSeleccionado
        var tempHasta = hastaSeleccionado

        // 1. Estado
        val etiquetasEstado = opcionesEstado.map { it.etiqueta }
        sheetBinding.spinnerEstado.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, etiquetasEstado)
        )
        val opcionActual = opcionesEstado.find { it.valor == tempEstado } ?: opcionesEstado.first()
        sheetBinding.spinnerEstado.setText(opcionActual.etiqueta, false)
        sheetBinding.spinnerEstado.setOnItemClickListener { _, _, position, _ ->
            tempEstado = opcionesEstado.getOrNull(position)?.valor
        }

        // 2. Conductor
        val itemsConductor = mutableListOf<ElementoFiltro>()
        itemsConductor.add(ElementoFiltro(null, getString(R.string.todos_los_conductores)))
        itemsConductor.addAll(listaConductores.map { ElementoFiltro(it.id, it.usuario?.name ?: "Conductor #${it.id}") })

        val adapterConductor = FiltroAdapter(requireContext(), android.R.layout.simple_list_item_1, itemsConductor) { it.nombre }
        sheetBinding.spinnerConductor.setAdapter(adapterConductor)

        val conductorInicial = itemsConductor.find { it.id == tempConductorId } ?: itemsConductor.first()
        var ultimoNombreValidoConductor = conductorInicial.nombre
        sheetBinding.spinnerConductor.setText(ultimoNombreValidoConductor, false)

        sheetBinding.spinnerConductor.setOnItemClickListener { adapterView: AdapterView<*>, _, position: Int, _ ->
            val seleccionado = adapterView.getItemAtPosition(position) as? ElementoFiltro
            if (seleccionado != null) {
                tempConductorId = seleccionado.id
                ultimoNombreValidoConductor = seleccionado.nombre
                sheetBinding.spinnerConductor.setText(seleccionado.nombre, false)
            }
        }

        sheetBinding.spinnerConductor.setOnClickListener {
            sheetBinding.spinnerConductor.showDropDown()
        }

        sheetBinding.spinnerConductor.setOnFocusChangeListener { _, hasFocus: Boolean ->
            if (hasFocus) {
                sheetBinding.spinnerConductor.showDropDown()
            } else {
                val textoIngresado = sheetBinding.spinnerConductor.text?.toString()?.trim().orEmpty()
                if (textoIngresado.isEmpty()) {
                    tempConductorId = null
                    ultimoNombreValidoConductor = itemsConductor.first().nombre
                    sheetBinding.spinnerConductor.setText(ultimoNombreValidoConductor, false)
                } else {
                    val match = itemsConductor.find {
                        FiltroAdapter.normalizarTexto(it.nombre) == FiltroAdapter.normalizarTexto(textoIngresado)
                    }
                    if (match != null) {
                        tempConductorId = match.id
                        ultimoNombreValidoConductor = match.nombre
                        sheetBinding.spinnerConductor.setText(match.nombre, false)
                    } else {
                        sheetBinding.spinnerConductor.setText(ultimoNombreValidoConductor, false)
                    }
                }
            }
        }

        // 3. Gasolinera
        val itemsGasolinera = mutableListOf<ElementoFiltro>()
        itemsGasolinera.add(ElementoFiltro(null, getString(R.string.todas_las_gasolineras)))
        itemsGasolinera.addAll(listaGasolineras.map {
            ElementoFiltro(it.id, if (!it.direccion.isNullOrBlank()) "${it.nombre} (${it.direccion})" else it.nombre)
        })

        val adapterGasolinera = FiltroAdapter(requireContext(), android.R.layout.simple_list_item_1, itemsGasolinera) { it.nombre }
        sheetBinding.spinnerGasolinera.setAdapter(adapterGasolinera)

        val gasolineraInicial = itemsGasolinera.find { it.id == tempGasolineraId } ?: itemsGasolinera.first()
        var ultimoNombreValidoGasolinera = gasolineraInicial.nombre
        sheetBinding.spinnerGasolinera.setText(ultimoNombreValidoGasolinera, false)

        sheetBinding.spinnerGasolinera.setOnItemClickListener { adapterView: AdapterView<*>, _, position: Int, _ ->
            val seleccionado = adapterView.getItemAtPosition(position) as? ElementoFiltro
            if (seleccionado != null) {
                tempGasolineraId = seleccionado.id
                ultimoNombreValidoGasolinera = seleccionado.nombre
                sheetBinding.spinnerGasolinera.setText(seleccionado.nombre, false)
            }
        }

        sheetBinding.spinnerGasolinera.setOnClickListener {
            sheetBinding.spinnerGasolinera.showDropDown()
        }

        sheetBinding.spinnerGasolinera.setOnFocusChangeListener { _, hasFocus: Boolean ->
            if (hasFocus) {
                sheetBinding.spinnerGasolinera.showDropDown()
            } else {
                val textoIngresado = sheetBinding.spinnerGasolinera.text?.toString()?.trim().orEmpty()
                if (textoIngresado.isEmpty()) {
                    tempGasolineraId = null
                    ultimoNombreValidoGasolinera = itemsGasolinera.first().nombre
                    sheetBinding.spinnerGasolinera.setText(ultimoNombreValidoGasolinera, false)
                } else {
                    val match = itemsGasolinera.find {
                        FiltroAdapter.normalizarTexto(it.nombre) == FiltroAdapter.normalizarTexto(textoIngresado)
                    }
                    if (match != null) {
                        tempGasolineraId = match.id
                        ultimoNombreValidoGasolinera = match.nombre
                        sheetBinding.spinnerGasolinera.setText(match.nombre, false)
                    } else {
                        sheetBinding.spinnerGasolinera.setText(ultimoNombreValidoGasolinera, false)
                    }
                }
            }
        }

        // 4. Fechas
        val actualizarTextoFechas = {
            if (tempDesde != null && tempHasta != null) {
                sheetBinding.inputRangoFechas.setText(formatearRangoChip(tempDesde!!, tempHasta!!))
            } else {
                sheetBinding.inputRangoFechas.setText("")
            }
        }
        actualizarTextoFechas()

        sheetBinding.inputRangoFechas.setOnClickListener {
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText(R.string.rango_de_fechas)
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                val startUtc = selection.first
                val endUtc = selection.second
                if (startUtc != null && endUtc != null) {
                    val sdfApi = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
                    tempDesde = sdfApi.format(Date(startUtc))
                    tempHasta = sdfApi.format(Date(endUtc))
                    actualizarTextoFechas()
                }
            }
            picker.show(childFragmentManager, "DATE_RANGE_PICKER")
        }

        // Boton Limpiar
        sheetBinding.btnLimpiarBottomSheet.setOnClickListener {
            tempEstado = null
            tempConductorId = null
            tempGasolineraId = null
            tempDesde = null
            tempHasta = null

            ultimoNombreValidoConductor = itemsConductor.first().nombre
            ultimoNombreValidoGasolinera = itemsGasolinera.first().nombre

            sheetBinding.spinnerEstado.setText(opcionesEstado.first().etiqueta, false)
            sheetBinding.spinnerConductor.setText(ultimoNombreValidoConductor, false)
            sheetBinding.spinnerGasolinera.setText(ultimoNombreValidoGasolinera, false)
            sheetBinding.inputRangoFechas.setText("")
        }

        // Boton Aplicar
        sheetBinding.btnAplicarBottomSheet.setOnClickListener {
            val txtConductor = sheetBinding.spinnerConductor.text?.toString()?.trim().orEmpty()
            if (txtConductor.isEmpty()) {
                tempConductorId = null
            } else {
                val match = itemsConductor.find {
                    FiltroAdapter.normalizarTexto(it.nombre) == FiltroAdapter.normalizarTexto(txtConductor)
                }
                if (match != null) tempConductorId = match.id
            }

            val txtGasolinera = sheetBinding.spinnerGasolinera.text?.toString()?.trim().orEmpty()
            if (txtGasolinera.isEmpty()) {
                tempGasolineraId = null
            } else {
                val match = itemsGasolinera.find {
                    FiltroAdapter.normalizarTexto(it.nombre) == FiltroAdapter.normalizarTexto(txtGasolinera)
                }
                if (match != null) tempGasolineraId = match.id
            }

            estadoSeleccionado = tempEstado
            conductorIdSeleccionado = tempConductorId
            gasolineraIdSeleccionada = tempGasolineraId
            desdeSeleccionado = tempDesde
            hastaSeleccionado = tempHasta

            dialog.dismiss()
            aplicarFiltros()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
