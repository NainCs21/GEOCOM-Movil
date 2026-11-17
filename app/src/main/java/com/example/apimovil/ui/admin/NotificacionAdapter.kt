package com.example.apimovil.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.apimovil.databinding.ItemNotificacionBinding

// Modelo para mostrar elementos de notificaciones
data class NotificacionItem(val ordenId: Int, val titulo: String, val cuerpo: String, val hora: String)

// Adapter para la lista de avisos y notificaciones
class NotificacionAdapter(
    private val items: List<NotificacionItem>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<NotificacionAdapter.Holder>() {

    inner class Holder(val binding: ItemNotificacionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemNotificacionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.binding.textTituloNotif.text = item.titulo
        holder.binding.textCuerpoNotif.text = item.cuerpo
        holder.binding.textHoraNotif.text = item.hora
        holder.binding.root.setOnClickListener { onItemClick(item.ordenId) }
    }
}
