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
    "completada", "completada_tarde" -> ContextCompat.getColor(context, R.color.estado_completada)
    "vencida" -> ContextCompat.getColor(context, R.color.estado_vencida)
    "abierta_tarde" -> ContextCompat.getColor(context, R.color.estado_vencida)
    else -> ContextCompat.getColor(context, R.color.texto_secundario)
}

// Retorna el fondo para el badge segun el estado
fun fondoBadgeDeEstado(estado: String): Int = when (estado) {
    "pendiente" -> R.drawable.bg_badge_pendiente
    "abierta" -> R.drawable.bg_badge_abierta
    "completada", "completada_tarde" -> R.drawable.bg_badge_completada
    "vencida" -> R.drawable.bg_badge_vencida
    "abierta_tarde" -> R.drawable.bg_badge_abierta
    else -> R.drawable.bg_badge_pendiente
}

// Retorna el texto del estado de forma clara
fun etiquetaDeEstado(context: Context, estado: String): String = when (estado) {
    "pendiente" -> context.getString(R.string.pendiente_de_retirar_badge)
    "abierta" -> context.getString(R.string.abierta_badge)
    "completada" -> context.getString(R.string.completada_badge)
    "vencida" -> context.getString(R.string.vencida_badge)
    "abierta_tarde" -> context.getString(R.string.abierta_tarde_badge)
    "completada_tarde" -> context.getString(R.string.completada_tarde_badge)
    else -> estado.uppercase()
}

// Adapter para la lista de ordenes en RecyclerView
class OrdenAdapter(
    private val mostrarConductor: Boolean = false,
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
        val gasolinera = orden.gasolinera?.nombre ?: ctx.getString(R.string.gasolinera_por_definir)
        val numero = "#${orden.numero_orden}"
        val nombreConductor = orden.conductor?.usuario?.name
        if (mostrarConductor && !nombreConductor.isNullOrBlank()) {
            // Admin/contabilidad: lo importante es de quién es la orden
            holder.binding.textTitulo.text = nombreConductor
            holder.binding.textSubtitulo.text = "$numero · $gasolinera"
        } else {
            // Pantallas del conductor: igual que antes, número arriba y gasolinera abajo
            holder.binding.textTitulo.text = numero
            holder.binding.textSubtitulo.text = gasolinera
        }
        holder.binding.textEstado.text = etiquetaDeEstado(ctx, orden.estado)
        holder.binding.textEstado.setTextColor(colorDeEstado(ctx, orden.estado))
        holder.binding.textEstado.setBackgroundResource(fondoBadgeDeEstado(orden.estado))
        holder.binding.viewBarraEstado.setBackgroundColor(colorDeEstado(ctx, orden.estado))
        holder.binding.root.setOnClickListener { onClick(orden) }
    }
}
