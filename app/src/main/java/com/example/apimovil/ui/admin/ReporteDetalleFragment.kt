package com.example.apimovil.ui.admin

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import com.example.apimovil.data.models.PuntoMes
import com.example.apimovil.data.models.ReporteConductorDetalleResp
import com.example.apimovil.data.models.ReporteGasolineraDetalleResp
import com.example.apimovil.data.models.ResumenReporte
import com.example.apimovil.databinding.FragmentReporteDetalleBinding
import com.example.apimovil.ui.common.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

class ReporteDetalleFragment : Fragment(R.layout.fragment_reporte_detalle) {

    private val model: AdminViewModel by viewModels()
    private var _binding: FragmentReporteDetalleBinding? = null
    private val binding get() = _binding!!

    private lateinit var ordenAdapter: OrdenReporteAdapter

    private var tipo: String = "conductor"
    private var entidadId: Int = -1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentReporteDetalleBinding.bind(view)

        tipo = arguments?.getString("tipo") ?: "conductor"
        entidadId = arguments?.getInt("entidadId") ?: -1

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

        ordenAdapter = OrdenReporteAdapter { orden ->
            val bundle = Bundle().apply { putInt("ordenId", orden.orden_id) }
            findNavController().navigate(R.id.ordenDetalleAdminFragment, bundle)
        }
        binding.recyclerOrdenes.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerOrdenes.adapter = ordenAdapter

        binding.btnPeriodo.setOnClickListener {
            mostrarSelectorPeriodo(this) { desde, hasta, etiqueta ->
                model.periodoSeleccionadoEtiqueta = etiqueta
                binding.btnPeriodo.text = etiqueta
                cargarReporte(desde, hasta)
            }
        }

        binding.btnExportarPdf.setOnClickListener {
            val respC = model.detalleConductor.value.data
            val respG = model.detalleGasolinera.value.data

            viewLifecycleOwner.lifecycleScope.launch {
                val pdfFile = if (tipo == "conductor" && respC != null) {
                    generarReporteDetallePdf(
                        context = requireContext(),
                        tipo = "conductor",
                        nombre = respC.conductor.nombre,
                        subtitulo = formatoCargo(respC.conductor.cargo),
                        textoPeriodo = model.periodoSeleccionadoEtiqueta,
                        nombreGenerador = SessionManager.nombre,
                        resumen = respC.resumen,
                        porMes = respC.por_mes,
                        porCombustible = respC.por_combustible,
                        ranking = respC.por_gasolinera,
                        porVehiculo = respC.por_vehiculo,
                        ordenes = respC.ordenes,
                        entidadIdOrNombre = respC.conductor.id.toString()
                    )
                } else if (tipo == "gasolinera" && respG != null) {
                    generarReporteDetallePdf(
                        context = requireContext(),
                        tipo = "gasolinera",
                        nombre = respG.gasolinera.nombre,
                        subtitulo = respG.gasolinera.direccion,
                        textoPeriodo = model.periodoSeleccionadoEtiqueta,
                        nombreGenerador = SessionManager.nombre,
                        resumen = respG.resumen,
                        porMes = respG.por_mes,
                        porCombustible = respG.por_combustible,
                        ranking = respG.por_conductor,
                        ordenes = respG.ordenes,
                        entidadIdOrNombre = respG.gasolinera.id.toString()
                    )
                } else {
                    null
                }

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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (tipo == "conductor") {
                    model.detalleConductor.collect { estado ->
                        binding.progress.isVisible = estado.loading
                        binding.textError.isVisible = estado.error != null
                        binding.textError.text = estado.error
                        estado.data?.let { pintarDetalleConductor(it) }
                    }
                } else {
                    model.detalleGasolinera.collect { estado ->
                        binding.progress.isVisible = estado.loading
                        binding.textError.isVisible = estado.error != null
                        binding.textError.text = estado.error
                        estado.data?.let { pintarDetalleGasolinera(it) }
                    }
                }
            }
        }

        cargarReporte(model.desdePeriodo, model.hastaPeriodo)
    }

    private fun cargarReporte(desde: String?, hasta: String?) {
        if (tipo == "conductor") {
            model.cargarDetalleConductor(entidadId, desde, hasta)
        } else {
            model.cargarDetalleGasolinera(entidadId, desde, hasta)
        }
    }

    private fun pintarCombustible(resumen: ResumenReporte, porMes: List<PuntoMes>) {
        binding.textGalonesTotal.text = String.format(Locale.US, "%,.1f", resumen.galones_total)
        binding.textPrecioPromedio.text = resumen.precio_promedio_galon
            ?.let { String.format(Locale.US, "L %,.2f/gal", it) } ?: "N/D"
        val galonesMes = porMes.sumOf { it.galones_total }
        binding.textEquivaleGalonesMes.isVisible = galonesMes > 0
        if (galonesMes > 0) {
            binding.textEquivaleGalonesMes.text = getString(R.string.equivale_a_galones, galonesMes)
        }
    }

    private fun pintarDetalleConductor(resp: ReporteConductorDetalleResp) {
        binding.textNombre.text = resp.conductor.nombre
        binding.textSubtitulo.text = formatoCargo(resp.conductor.cargo)
        binding.textInactivo.isVisible = !resp.conductor.activo

        val sinDatos = resp.resumen.cantidad_ordenes == 0
        binding.layoutContenidoDatos.isVisible = !sinDatos
        binding.textSinDatos.isVisible = sinDatos
        binding.btnExportarPdf.isEnabled = !sinDatos

        binding.textGastoTotal.text = String.format(Locale.US, "L %,.2f", resp.resumen.monto_total)
        binding.textCantidadOrdenes.text = resp.resumen.cantidad_ordenes.toString()
        binding.textPromedioOrden.text = String.format(Locale.US, "L %,.2f", resp.resumen.promedio_orden)
        binding.textCumplimiento.text = String.format(Locale.US, "%.1f%%", resp.resumen.cumplimiento_pct)
        binding.textTardesVencidas.text = getString(R.string.tardes_vencidas_formato, resp.resumen.completadas_tarde, resp.resumen.vencidas)

        pintarCombustible(resp.resumen, resp.por_mes)

        // Graficas
        binding.chartMeses.setDatos(resp.por_mes.map { BarChartView.Barra(formatearMes(it.mes), it.monto_total) })
        binding.chartCombustible.horizontal = true
        binding.chartCombustible.setDatos(resp.por_combustible.map {
            val tipoComb = if (it.tipo_combustible.lowercase().contains("gas")) "Gasolina" else "Diésel"
            BarChartView.Barra(tipoComb, it.monto_total)
        })

        binding.textTituloRanking.text = getString(R.string.gasolineras_donde_cargo)
        binding.chartRanking.horizontal = true
        binding.chartRanking.setDatos(resp.por_gasolinera.map { BarChartView.Barra(it.nombre, it.monto_total) })

        val vehiculos = resp.por_vehiculo
        binding.cardVehiculos.isVisible = vehiculos.isNotEmpty()
        binding.contenedorVehiculos.removeAllViews()
        val density = resources.displayMetrics.density
        vehiculos.forEach { v ->
            val layout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 0, 0, (12 * density).toInt())
            }
            val tvTitulo = TextView(requireContext()).apply {
                text = "${v.placa} · ${v.descripcion}"
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(requireContext(), R.color.texto_principal))
            }
            val montoFmt = String.format(Locale.US, "%,.2f", v.monto_total)
            val galonesFmt = String.format(Locale.US, "%.1f", v.galones_total)
            val kmFmt = String.format(Locale.US, "%,.0f", v.km_total)
            val rendFmt = String.format(Locale.US, "%.1f", v.rendimiento_km_litro ?: 8.0)
            val tvSubtitulo = TextView(requireContext()).apply {
                text = "L $montoFmt · ≈ $galonesFmt gal · ≈ $kmFmt km · $rendFmt km/L"
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.texto_secundario))
            }
            layout.addView(tvTitulo)
            layout.addView(tvSubtitulo)
            binding.contenedorVehiculos.addView(layout)
        }

        ordenAdapter.submitList(resp.ordenes)
    }

    private fun pintarDetalleGasolinera(resp: ReporteGasolineraDetalleResp) {
        binding.textNombre.text = resp.gasolinera.nombre
        binding.textSubtitulo.text = resp.gasolinera.direccion ?: "—"
        binding.textInactivo.isVisible = !resp.gasolinera.activo

        val sinDatos = resp.resumen.cantidad_ordenes == 0
        binding.layoutContenidoDatos.isVisible = !sinDatos
        binding.textSinDatos.isVisible = sinDatos
        binding.btnExportarPdf.isEnabled = !sinDatos

        binding.textGastoTotal.text = String.format(Locale.US, "L %,.2f", resp.resumen.monto_total)
        binding.textCantidadOrdenes.text = resp.resumen.cantidad_ordenes.toString()
        binding.textPromedioOrden.text = String.format(Locale.US, "L %,.2f", resp.resumen.promedio_orden)
        binding.textCumplimiento.text = String.format(Locale.US, "%.1f%%", resp.resumen.cumplimiento_pct)
        binding.textTardesVencidas.text = getString(R.string.tardes_vencidas_formato, resp.resumen.completadas_tarde, resp.resumen.vencidas)

        pintarCombustible(resp.resumen, resp.por_mes)

        // Graficas
        binding.chartMeses.setDatos(resp.por_mes.map { BarChartView.Barra(formatearMes(it.mes), it.monto_total) })
        binding.chartCombustible.horizontal = true
        binding.chartCombustible.setDatos(resp.por_combustible.map {
            val tipoComb = if (it.tipo_combustible.lowercase().contains("gas")) "Gasolina" else "Diésel"
            BarChartView.Barra(tipoComb, it.monto_total)
        })

        binding.textTituloRanking.text = getString(R.string.conductores_que_la_usan)
        binding.chartRanking.horizontal = true
        val colores = PaletaGrafica.asignar(resp.por_conductor.map { it.id })
        binding.chartRanking.setDatos(resp.por_conductor.mapIndexed { i, c -> BarChartView.Barra(c.nombre, c.monto_total, colores[i]) })

        binding.cardVehiculos.isVisible = false

        ordenAdapter.submitList(resp.ordenes)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
