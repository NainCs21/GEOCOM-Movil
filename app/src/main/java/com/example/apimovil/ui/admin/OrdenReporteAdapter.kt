package com.example.apimovil.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apimovil.R
import com.example.apimovil.data.models.OrdenReporte
import com.example.apimovil.databinding.ItemOrdenReporteBinding
import com.example.apimovil.ui.common.*
import java.util.Locale

private val DIFF = object : DiffUtil.ItemCallback<OrdenReporte>() {
    override fun areItemsTheSame(old: OrdenReporte, new: OrdenReporte) = old.orden_id == new.orden_id
    override fun areContentsTheSame(old: OrdenReporte, new: OrdenReporte) = old == new
}

class OrdenReporteAdapter(
    private val onClick: (OrdenReporte) -> Unit
) : ListAdapter<OrdenReporte, OrdenReporteAdapter.Holder>(DIFF) {

    class Holder(val binding: ItemOrdenReporteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemOrdenReporteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val orden = getItem(position)
        val ctx = holder.binding.root.context
        holder.binding.textNumeroOrden.text = ctx.getString(R.string.orden_numero, orden.numero_orden)
        holder.binding.textDestino.text = "${orden.destino} (${orden.tipo_combustible})"
        holder.binding.textFecha.text = formatearFechaIsoShort(orden.fecha_completada)
        holder.binding.textMonto.text = String.format(Locale.US, "L %,.2f", orden.monto)

        if (orden.galones_estimados != null) {
            val gal = orden.galones_estimados
            val precio = orden.precio_galon
            val txtEst = if (precio != null) {
                String.format(Locale.US, "≈ %.1f gal · L %.2f/gal", gal, precio)
            } else {
                String.format(Locale.US, "≈ %.1f gal", gal)
            }
            holder.binding.textEstimacion.isVisible = true
            holder.binding.textEstimacion.text = txtEst
        } else {
            holder.binding.textEstimacion.isVisible = false
        }

        val estadoTxt = when (orden.estado) {
            "completada_tarde" -> ctx.getString(R.string.completada_tarde_badge)
            "completada" -> ctx.getString(R.string.completada_badge)
            else -> etiquetaDeEstado(ctx, orden.estado)
        }
        holder.binding.textEstado.text = estadoTxt
        holder.binding.textEstado.setTextColor(colorDeEstado(ctx, orden.estado))
        holder.binding.textEstado.setBackgroundResource(fondoBadgeDeEstado(orden.estado))

        holder.binding.root.setOnClickListener { onClick(orden) }
    }
}
