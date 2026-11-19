package com.example.apimovil.ui.admin

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apimovil.R
import com.example.apimovil.data.notificaciones.NotificacionApp
import com.example.apimovil.databinding.ItemNotificacionBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificacionListAdapter(
    private val onItemClick: (NotificacionApp) -> Unit
) : ListAdapter<NotificacionApp, NotificacionListAdapter.Holder>(DiffCallback()) {

    class Holder(val binding: ItemNotificacionBinding) : RecyclerView.ViewHolder(binding.root)

    class DiffCallback : DiffUtil.ItemCallback<NotificacionApp>() {
        override fun areItemsTheSame(oldItem: NotificacionApp, newItem: NotificacionApp): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: NotificacionApp, newItem: NotificacionApp): Boolean = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemNotificacionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        holder.binding.textTituloNotif.text = item.titulo
        holder.binding.textCuerpoNotif.text = item.cuerpo
        val dateStr = try {
            SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale("es", "HN")).format(Date(item.fechaMillis))
        } catch (_: Exception) {
            ""
        }
        holder.binding.textHoraNotif.text = dateStr

        if (!item.leida) {
            holder.binding.textTituloNotif.setTypeface(null, Typeface.BOLD)
            holder.binding.root.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.marca_claro))
        } else {
            holder.binding.textTituloNotif.setTypeface(null, Typeface.NORMAL)
            holder.binding.root.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.blanco))
        }

        holder.binding.root.setOnClickListener { onItemClick(item) }
    }
}
