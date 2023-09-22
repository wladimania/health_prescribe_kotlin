package com.example.health_prescribe.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.health_prescribe.R
import com.example.health_prescribe.model.FarmacoDisplay

class FarmacoAdapter(private val farmacos: MutableList<FarmacoDisplay>) : RecyclerView.Adapter<FarmacoAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tv_nombre_farmaco)
        val tvCantidad: TextView = itemView.findViewById(R.id.tv_cantidad)
        val tvAplicacion: TextView = itemView.findViewById(R.id.tv_aplicacion)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_farmaco, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val farmaco = farmacos[position]
        holder.tvNombre.text = farmaco.nombre_generico
        holder.tvCantidad.text = "Cantidad: ${farmaco.cantidad}"
        holder.tvAplicacion.text = "Aplicación: ${farmaco.aplicacion}"
    }

    override fun getItemCount(): Int = farmacos.size

    fun updateData(newFarmacos: List<FarmacoDisplay>) {
        // Actualiza solo los elementos que han cambiado
        for (i in farmacos.indices) {
            if (i < newFarmacos.size) {
                farmacos[i] = newFarmacos[i]
                notifyItemChanged(i)
            } else {
                // Si newFarmacos tiene menos elementos, quita los elementos extras
                farmacos.removeAt(i)
                notifyItemRemoved(i)
            }
        }

        // Si newFarmacos tiene más elementos, agrégales nuevos elementos al final
        if (farmacos.size < newFarmacos.size) {
            farmacos.addAll(newFarmacos.subList(farmacos.size, newFarmacos.size))
            notifyItemRangeInserted(farmacos.size, newFarmacos.size - farmacos.size)
        }
    }
}
