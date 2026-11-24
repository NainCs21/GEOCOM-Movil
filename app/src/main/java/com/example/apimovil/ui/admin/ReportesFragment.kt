package com.example.apimovil.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
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
import com.example.apimovil.databinding.FragmentReportesBinding
import com.example.apimovil.ui.common.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

class ReportesFragment : Fragment(R.layout.fragment_reportes) {

    private val model: AdminViewModel by viewModels()
    private var _binding: FragmentReportesBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentReportesBinding.bind(view)
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())

        val bottomNav = binding.root.findViewById<BottomNavigationView>(R.id.incluirNavInferior)
            ?: binding.root.findViewById<BottomNavigationView>(R.id.bottomNavAdmin)
        if (SessionManager.esAdmin) {
            bottomNav?.menu?.clear()
            bottomNav?.inflateMenu(R.menu.bottom_nav_admin)
            bottomNav?.configurarNavegacionAdmin(findNavController(), R.id.reportesFragment)
            bottomNav?.vincularBadgeAvisos(viewLifecycleOwner, R.id.nav_mas)
            bottomNav?.vincularBadgeSolicitudes(viewLifecycleOwner, R.id.nav_solicitudes, model.solicitudesPendientes.map { it.data?.size ?: 0 })
            model.cargarSolicitudesPendientes()
        } else {
            bottomNav?.menu?.clear()
            bottomNav?.inflateMenu(R.menu.bottom_nav_supervisor)
            bottomNav?.configurarNavegacionSupervisor(findNavController(), R.id.reportesFragment)
            bottomNav?.vincularBadgeAvisos(viewLifecycleOwner, R.id.nav_avisos)
        }

        binding.btnVolver.setOnClickListener { findNavController().navigateUp() }
        binding.btnPeriodo.text = model.periodoSeleccionadoEtiqueta
        actualizarBotonFiltroEntidad()

        binding.btnPeriodo.setOnClickListener {
            mostrarSelectorPeriodo(this) { desde, hasta, etiqueta ->
                model.periodoSeleccionadoEtiqueta = etiqueta
                binding.btnPeriodo.text = etiqueta
                model.cargarListasSinFiltro(desde, hasta)
                cargarReporteActual(desde, hasta)
                cargarResumenActual(desde, hasta)
            }
        }

        binding.btnFiltroEntidad.setOnClickListener {
            val porConductor = binding.toggleReporte.checkedButtonId == R.id.btnPorConductor
            if (porConductor) {
                val lista = model.conductoresSinFiltro.value.sortedBy { it.nombre_conductor }
                val nombres = mutableListOf(getString(R.string.todos_los_conductores))
                nombres.addAll(lista.map { it.nombre_conductor })
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.filtrar_por_conductor)
                    .setItems(nombres.toTypedArray()) { _, which ->
                        if (which == 0) {
                            model.conductorFiltroId = null
                            model.conductorFiltroNombre = null
                        } else {
                            val sel = lista.getOrNull(which - 1)
                            if (sel != null) {
                                model.conductorFiltroId = sel.conductor_id
                                model.conductorFiltroNombre = sel.nombre_conductor
                            }
                        }
                        actualizarBotonFiltroEntidad()
                        cargarReporteActual(model.desdePeriodo, model.hastaPeriodo)
                        cargarResumenActual(model.desdePeriodo, model.hastaPeriodo)
                    }
                    .show()
            } else {
                val lista = model.gasolinerasSinFiltro.value.sortedBy { it.nombre_gasolinera }
                val nombres = mutableListOf(getString(R.string.todas_las_gasolineras))
                nombres.addAll(lista.map { it.nombre_gasolinera })
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.filtrar_por_gasolinera)
                    .setItems(nombres.toTypedArray()) { _, which ->
                        if (which == 0) {
                            model.gasolineraFiltroId = null
                            model.gasolineraFiltroNombre = null
                        } else {
                            val sel = lista.getOrNull(which - 1)
                            if (sel != null) {
                                model.gasolineraFiltroId = sel.gasolinera_id
                                model.gasolineraFiltroNombre = sel.nombre_gasolinera
                            }
                        }
                        actualizarBotonFiltroEntidad()
                        cargarReporteActual(model.desdePeriodo, model.hastaPeriodo)
                        cargarResumenActual(model.desdePeriodo, model.hastaPeriodo)
                    }
                    .show()
            }
        }

        binding.btnExportarPdf.setOnClickListener {
            val esConductor = binding.toggleReporte.checkedButtonId == R.id.btnPorConductor
            val titulo = if (esConductor) getString(R.string.reporte_consumo_conductor) else getString(R.string.reporte_consumo_gasolinera)
            val columnaEntidad = if (esConductor) "Conductor" else "Gasolinera"
            val textoPeriodo = if (model.desdePeriodo.isNullOrBlank() || model.hastaPeriodo.isNullOrBlank()) {
                ""
            } else {
                "${formatearFechaIsoShort(model.desdePeriodo)} al ${formatearFechaIsoShort(model.hastaPeriodo)}"
            }

            val filas = if (esConductor) {
                model.reporteConductor.value.data.orEmpty().map { FilaReportePdf(it.nombre_conductor, it.cantidad_ordenes, it.monto_total) }
            } else {
                model.reporteGasolinera.value.data.orEmpty().map { FilaReportePdf(it.nombre_gasolinera, it.cantidad_ordenes, it.monto_total) }
            }

            if (filas.isEmpty()) return@setOnClickListener

            val totalOrdenes = filas.sumOf { it.cantidadOrdenes }
            val montoTotal = filas.sumOf { it.montoTotal }
            val nombreGenerador = SessionManager.nombre

            viewLifecycleOwner.lifecycleScope.launch {
                val pdfFile = generarReportePdf(
                    context = requireContext(),
                    tituloReporte = titulo,
                    textoPeriodo = textoPeriodo,
                    nombreGenerador = nombreGenerador,
                    nombreColumnaEntidad = columnaEntidad,
                    filas = filas,
                    totalOrdenes = totalOrdenes,
                    montoTotalGeneral = montoTotal,
                    esConductor = esConductor,
                    desde = model.desdePeriodo,
                    hasta = model.hastaPeriodo,
                    resumen = model.reporteResumen.value.data,
                    nombreEntidadFiltro = if (esConductor) model.conductorFiltroNombre else model.gasolineraFiltroNombre
                )

                if (pdfFile != null && pdfFile.exists()) {
                    try {
                        val uri = FileProvider.getUriForFile(
                            requireContext(),
                            "${requireContext().packageName}.fileprovider",
                            pdfFile
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(intent, "Compartir reporte"))
                    } catch (_: Exception) {
                        Toast.makeText(requireContext(), "No se pudo generar el PDF", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "No se pudo generar el PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.toggleReporte.check(R.id.btnPorConductor)
        model.cargarListasSinFiltro(model.desdePeriodo, model.hastaPeriodo)
        cargarReporteActual(model.desdePeriodo, model.hastaPeriodo)
        cargarResumenActual(model.desdePeriodo, model.hastaPeriodo)

        binding.btnPorConductor.setOnClickListener {
            binding.toggleReporte.check(R.id.btnPorConductor)
            actualizarBotonFiltroEntidad()
            cargarReporteActual(model.desdePeriodo, model.hastaPeriodo)
            cargarResumenActual(model.desdePeriodo, model.hastaPeriodo)
        }
        binding.btnPorGasolinera.setOnClickListener {
            binding.toggleReporte.check(R.id.btnPorGasolinera)
            actualizarBotonFiltroEntidad()
            cargarReporteActual(model.desdePeriodo, model.hastaPeriodo)
            cargarResumenActual(model.desdePeriodo, model.hastaPeriodo)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.reporteConductor.collect { estado ->
                    if (binding.toggleReporte.checkedButtonId == R.id.btnPorConductor) {
                        binding.progress.isVisible = estado.loading
                        val data = estado.data.orEmpty()
                        binding.textSinResultados.isVisible = (!estado.loading && estado.error == null && data.isEmpty())
                        binding.btnExportarPdf.isEnabled = !estado.loading && data.isNotEmpty()

                        binding.textTituloReporte.text = getString(R.string.reporte_consumo_conductor)
                        binding.textSubtituloPeriodo.text = if (model.desdePeriodo.isNullOrBlank() || model.hastaPeriodo.isNullOrBlank()) {
                            getString(R.string.ordenes_completadas_todo)
                        } else {
                            getString(R.string.ordenes_completadas_periodo, formatearFechaIsoShort(model.desdePeriodo), formatearFechaIsoShort(model.hastaPeriodo))
                        }

                        val totalOrdenes = data.sumOf { it.cantidad_ordenes }
                        val totalMonto = data.sumOf { it.monto_total }
                        val totalMontoFmt = String.format(Locale.US, "%,.2f", totalMonto)
                        binding.textTotalesGenerales.text = getString(R.string.resumen_ordenes_monto, totalOrdenes, totalMontoFmt)

                        val items = data.map {
                            val montoFmt = String.format(Locale.US, "%,.2f", it.monto_total)
                            FilaDoble(
                                id = it.conductor_id,
                                titulo = it.nombre_conductor,
                                subtitulo = "${it.cantidad_ordenes} órdenes · L $montoFmt"
                            )
                        }
                        binding.recycler.adapter = FilaDobleAdapter(items) { id, _ ->
                            val rep = data.find { it.conductor_id == id }
                            if (rep != null) {
                                model.conductorFiltroId = rep.conductor_id
                                model.conductorFiltroNombre = rep.nombre_conductor
                                val bundle = Bundle().apply {
                                    putString("tipo", "conductor")
                                    putInt("entidadId", rep.conductor_id)
                                }
                                findNavController().navigate(R.id.action_reportes_to_detalle, bundle)
                            }
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.reporteGasolinera.collect { estado ->
                    if (binding.toggleReporte.checkedButtonId == R.id.btnPorGasolinera) {
                        binding.progress.isVisible = estado.loading
                        val data = estado.data.orEmpty()
                        binding.textSinResultados.isVisible = (!estado.loading && estado.error == null && data.isEmpty())
                        binding.btnExportarPdf.isEnabled = !estado.loading && data.isNotEmpty()

                        binding.textTituloReporte.text = getString(R.string.reporte_consumo_gasolinera)
                        binding.textSubtituloPeriodo.text = if (model.desdePeriodo.isNullOrBlank() || model.hastaPeriodo.isNullOrBlank()) {
                            getString(R.string.ordenes_completadas_todo)
                        } else {
                            getString(R.string.ordenes_completadas_periodo, formatearFechaIsoShort(model.desdePeriodo), formatearFechaIsoShort(model.hastaPeriodo))
                        }

                        val totalOrdenes = data.sumOf { it.cantidad_ordenes }
                        val totalMonto = data.sumOf { it.monto_total }
                        val totalMontoFmt = String.format(Locale.US, "%,.2f", totalMonto)
                        binding.textTotalesGenerales.text = getString(R.string.resumen_ordenes_monto, totalOrdenes, totalMontoFmt)

                        val items = data.map {
                            val montoFmt = String.format(Locale.US, "%,.2f", it.monto_total)
                            FilaDoble(
                                id = it.gasolinera_id,
                                titulo = it.nombre_gasolinera,
                                subtitulo = "${it.cantidad_ordenes} órdenes · L $montoFmt"
                            )
                        }
                        binding.recycler.adapter = FilaDobleAdapter(items) { id, _ ->
                            val rep = data.find { it.gasolinera_id == id }
                            if (rep != null) {
                                model.gasolineraFiltroId = rep.gasolinera_id
                                model.gasolineraFiltroNombre = rep.nombre_gasolinera
                                val bundle = Bundle().apply {
                                    putString("tipo", "gasolinera")
                                    putInt("entidadId", rep.gasolinera_id)
                                }
                                findNavController().navigate(R.id.action_reportes_to_detalle, bundle)
                            }
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.reporteResumen.collect { estado ->
                    val resp = estado.data
                    if (resp != null) {
                        binding.textPromedioYCumplimiento.text = getString(
                            R.string.promedio_cumplimiento_formato,
                            resp.resumen.promedio_orden,
                            resp.resumen.cumplimiento_pct
                        )
                        if (resp.resumen.precio_promedio_galon != null) {
                            binding.textGalonesYPromedio.isVisible = true
                            binding.textGalonesYPromedio.text = getString(
                                R.string.galones_y_promedio_formato,
                                resp.resumen.galones_total,
                                resp.resumen.precio_promedio_galon
                            )
                        } else {
                            binding.textGalonesYPromedio.isVisible = false
                        }

                        binding.chartMesesGeneral.setDatos(resp.por_mes.map { BarChartView.Barra(formatearMes(it.mes), it.monto_total) })
                        val totalGalonesMes = resp.por_mes.sumOf { it.galones_total }
                        if (totalGalonesMes > 0) {
                            binding.textEquivaleGalonesGeneral.isVisible = true
                            binding.textEquivaleGalonesGeneral.text = getString(R.string.equivale_a_galones, totalGalonesMes)
                        } else {
                            binding.textEquivaleGalonesGeneral.isVisible = false
                        }

                        val porConductor = binding.toggleReporte.checkedButtonId == R.id.btnPorConductor
                        if (porConductor) {
                            binding.textTituloRankingGeneral.text = getString(R.string.top_conductores_por_gasto)
                            binding.chartRankingGeneral.horizontal = true
                            binding.chartRankingGeneral.setDatos(resp.top_conductores.take(8).map { BarChartView.Barra(it.nombre, it.monto_total) })
                        } else {
                            binding.textTituloRankingGeneral.text = getString(R.string.top_gasolineras_por_gasto)
                            binding.chartRankingGeneral.horizontal = true
                            binding.chartRankingGeneral.setDatos(resp.top_gasolineras.take(8).map { BarChartView.Barra(it.nombre, it.monto_total) })
                        }
                    }
                }
            }
        }
    }

    private fun actualizarBotonFiltroEntidad() {
        val porConductor = binding.toggleReporte.checkedButtonId == R.id.btnPorConductor
        if (porConductor) {
            binding.btnFiltroEntidad.text = model.conductorFiltroNombre ?: getString(R.string.todos_los_conductores)
        } else {
            binding.btnFiltroEntidad.text = model.gasolineraFiltroNombre ?: getString(R.string.todas_las_gasolineras)
        }
    }

    private fun cargarReporteActual(desde: String?, hasta: String?) {
        if (binding.toggleReporte.checkedButtonId == R.id.btnPorConductor) {
            model.cargarReportePorConductor(desde, hasta)
        } else {
            model.cargarReportePorGasolinera(desde, hasta)
        }
    }

    private fun cargarResumenActual(desde: String?, hasta: String?) {
        val porConductor = binding.toggleReporte.checkedButtonId == R.id.btnPorConductor
        model.cargarReporteResumen(desde, hasta, porConductor)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
