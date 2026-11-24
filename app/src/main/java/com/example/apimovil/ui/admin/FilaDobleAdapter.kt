package com.example.apimovil.ui.admin

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.apimovil.R
import com.example.apimovil.databinding.ItemFilaDobleBinding

// Modelo simple para mostrar filas de dos lineas de texto
data class FilaDoble(
    val id: Int = 0,
    val titulo: String,
    val subtitulo: String,
    val iconRes: Int = R.drawable.ic_nav_catalogos,
    val tintRes: Int? = null
)

// Adapter para listas de catalogos y reportes simples
class FilaDobleAdapter(
    private val items: List<FilaDoble>,
    private val onItemClick: ((Int, String) -> Unit)? = null
) : RecyclerView.Adapter<FilaDobleAdapter.Holder>() {

    class Holder(val binding: ItemFilaDobleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemFilaDobleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val context = holder.binding.root.context
        holder.binding.textTitulo.text = item.titulo
        holder.binding.textSubtitulo.text = item.subtitulo
        holder.binding.imgIcono.setImageResource(item.iconRes)

        val tintColor = if (item.tintRes != null) {
            ContextCompat.getColor(context, item.tintRes)
        } else {
            ContextCompat.getColor(context, R.color.marca)
        }
        holder.binding.imgIcono.imageTintList = ColorStateList.valueOf(tintColor)

        holder.binding.root.setOnClickListener {
            onItemClick?.invoke(item.id, item.titulo)
        }
    }
}
