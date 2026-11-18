package com.example.apimovil.ui.common

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import java.text.Normalizer

// Elemento generico para opciones con id e identificador de texto
data class ElementoFiltro(val id: Int?, val nombre: String) {
    override fun toString(): String = nombre
}

// Adapter personalizado para AutoCompleteTextView que permite buscar coincidencias
// en cualquier parte del texto sin importar mayusculas ni acentos (tildes).
class FiltroAdapter<T>(
    context: Context,
    resource: Int,
    private val allItems: List<T>,
    private val getLabel: (T) -> String
) : ArrayAdapter<T>(context, resource, ArrayList(allItems)) {

    private val filterImpl = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            val query = constraint?.toString().orEmpty().trim()

            val filteredList = if (query.isEmpty()) {
                allItems
            } else {
                val queryNormalizada = normalizarTexto(query)
                allItems.filter { item ->
                    val labelNormalizada = normalizarTexto(getLabel(item))
                    labelNormalizada.contains(queryNormalizada)
                }
            }

            results.values = filteredList
            results.count = filteredList.size
            return results
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            clear()
            if (results != null && results.count > 0) {
                addAll(results.values as List<T>)
            }
            notifyDataSetChanged()
        }

        override fun convertResultToString(resultValue: Any?): CharSequence {
            @Suppress("UNCHECKED_CAST")
            return if (resultValue != null) getLabel(resultValue as T) else ""
        }
    }

    override fun getFilter(): Filter {
        return filterImpl
    }

    companion object {
        fun normalizarTexto(texto: String): String {
            val sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            return sinAcentos.lowercase()
        }
    }
}
