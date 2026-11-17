package com.example.apimovil.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apimovil.R
import com.example.apimovil.data.models.Orden
import com.example.apimovil.databinding.ItemOrdenBinding

private val DIFF = object : DiffUtil.ItemCallback<Orden>() {
    override fun areItemsTheSame(old: Orden, new: Orden) = old.id == new.id
    override fun areContentsTheSame(old: Orden, new: Orden) = old == new
}

// Retorna el color de texto para el badge segun el estado
fun colorDeEstado(context: Context, estado: String): Int = when (estado) {
    "pendiente" -> ContextCompat.getColor(context, R.color.estado_pendiente)
    "abierta" -> ContextCompat.getColor(context, R.color.estado_abierta)
    "completada" -> ContextCompat.getColor(context, R.color.estado_completada)
    "vencida" -> ContextCompat.getColor(context, R.color.estado_vencida)
    "rehabilitada" -> ContextCompat.getColor(context, R.color.marca)
    "tardio", "completado_tardio" -> ContextCompat.getColor(context, R.color.estado_vencida)
    else -> ContextCompat.getColor(context, R.color.texto_secundario)
}

// Retorna el fondo para el badge segun el estado
fun fondoBadgeDeEstado(estado: String): Int = when (estado) {
    "pendiente" -> R.drawable.bg_badge_pendiente
    "abierta" -> R.drawable.bg_badge_abierta
    "completada" -> R.drawable.bg_badge_completada
    "vencida" -> R.drawable.bg_badge_vencida
    "rehabilitada" -> R.drawable.bg_badge_abierta
    "tardio", "completado_tardio" -> R.drawable.bg_badge_vencida
    else -> R.drawable.bg_badge_pendiente
}

// Retorna el texto del estado de forma clara
fun etiquetaDeEstado(context: Context, estado: String): String = when (estado) {
    "pendiente" -> context.getString(R.string.pendiente_de_retirar_badge)
    "abierta" -> context.getString(R.string.abierta_badge)
    "completada" -> context.getString(R.string.completada_badge)
    "vencida" -> context.getString(R.string.vencida_badge)
    "rehabilitada" -> context.getString(R.string.rehabilitada_badge)
    "tardio", "completado_tardio" -> context.getString(R.string.tardio_badge)
    else -> estado.uppercase()
}

// Adapter para la lista de ordenes en RecyclerView
class OrdenAdapter(
    private val onClick: (Orden) -> Unit
) : ListAdapter<Orden, OrdenAdapter.Holder>(DIFF) {

    inner class Holder(val binding: ItemOrdenBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemOrdenBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val orden = getItem(position)
        val ctx = holder.binding.root.context
        holder.binding.textNumeroOrden.text = ctx.getString(R.string.orden_numero, orden.numero_orden)
        holder.binding.textGasolinera.text = orden.gasolinera?.nombre ?: ctx.getString(R.string.gasolinera_por_definir)
        holder.binding.textEstado.text = etiquetaDeEstado(ctx, orden.estado)
        holder.binding.textEstado.setTextColor(colorDeEstado(ctx, orden.estado))
        holder.binding.textEstado.setBackgroundResource(fondoBadgeDeEstado(orden.estado))
        holder.binding.root.setOnClickListener { onClick(orden) }
    }
}
