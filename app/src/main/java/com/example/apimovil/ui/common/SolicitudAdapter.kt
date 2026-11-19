package com.example.apimovil.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apimovil.R
import com.example.apimovil.data.models.Solicitud
import com.example.apimovil.databinding.ItemSolicitudBinding

private val DIFF = object : DiffUtil.ItemCallback<Solicitud>() {
    override fun areItemsTheSame(old: Solicitud, new: Solicitud) = old.id == new.id
    override fun areContentsTheSame(old: Solicitud, new: Solicitud) = old == new
}

private fun etiquetaDestino(tipo: String): String = when (tipo) {
    "vehiculo" -> "VEHÍCULO"
    "mgp" -> "MGP"
    "ambos" -> "VEHÍCULO + MGP"
    else -> tipo.uppercase()
}

// Calcula la antiguedad de la solicitud
private fun antiguedad(context: Context, createdAt: String?): String {
    if (createdAt.isNullOrBlank()) return ""
    return try {
        val fecha = parseFechaIso(createdAt).time
        val minutos = ((System.currentTimeMillis() - fecha) / 60000).coerceAtLeast(0)
        when {
            minutos < 60 -> context.getString(R.string.hace_n_min, minutos.toInt())
            else -> context.getString(R.string.hace_n_h, (minutos / 60).toInt())
        }
    } catch (_: Exception) {
        ""
    }
}

// Adapter para la lista de solicitudes pendientes o del conductor
class SolicitudAdapter(
    private val mostrarAccionAsignar: Boolean = true,
    private val onRechazar: ((Solicitud) -> Unit)? = null,
    private val onEditar: ((Solicitud) -> Unit)? = null,
    private val onClick: (Solicitud) -> Unit
) : ListAdapter<Solicitud, SolicitudAdapter.Holder>(DIFF) {

    inner class Holder(val binding: ItemSolicitudBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemSolicitudBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val solicitud = getItem(position)
        val ctx = holder.binding.root.context

        holder.binding.textCodigoSolicitud.text = ctx.getString(R.string.solicitud_codigo, solicitud.id)
        holder.binding.textAntiguedad.text = antiguedad(ctx, solicitud.created_at)
        holder.binding.textConductor.text = solicitud.conductor?.usuario?.name
            ?: ctx.getString(R.string.conductor_numero, solicitud.conductor_id)
        holder.binding.textDestino.text = etiquetaDestino(solicitud.tipo_destino)
        holder.binding.textActividad.text = solicitud.descripcion_actividad
        holder.binding.textAsignarMonto.isVisible = mostrarAccionAsignar
        holder.binding.textRechazar.isVisible = mostrarAccionAsignar
        holder.binding.textRechazar.setOnClickListener { onRechazar?.invoke(solicitud) }

        holder.binding.textEditar.isVisible = (onEditar != null && solicitud.estado == "pendiente")
        holder.binding.textEditar.setOnClickListener { onEditar?.invoke(solicitud) }

        holder.binding.root.setOnClickListener { onClick(solicitud) }
    }
}
