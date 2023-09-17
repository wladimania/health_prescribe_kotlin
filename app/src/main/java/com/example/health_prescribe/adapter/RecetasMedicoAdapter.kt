package com.example.health_prescribe

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.health_prescribe.model.recetas

class RecetasMedicoAdapter(private val recetasList: List<recetas>) : RecyclerView.Adapter<RecetasMedicoAdapter.RecetaViewHolder>() {

    inner class RecetaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombrePaciente: TextView = itemView.findViewById(R.id.tv_nombre_apellidos_paciente)
        val tvFechaGeneracion: TextView = itemView.findViewById(R.id.tv_fecha_generacion)
        val tvFechaEntrega: TextView = itemView.findViewById(R.id.tv_fecha_entrega)
        val tvEstadoReceta: TextView = itemView.findViewById(R.id.tv_estado_receta)
    }
    init {
        Log.d("RecetasMedicoAdapter", "Número de recetas en el adaptador: ${recetasList.size}")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecetaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_receta, parent, false)
        return RecetaViewHolder(view)
    }

    override fun getItemCount(): Int {
        return recetasList.size
    }


    override fun onBindViewHolder(holder: RecetaViewHolder, position: Int) {
        val receta = recetasList[position]
        holder.tvNombrePaciente.text = "Nombre del Paciente:${receta.nombreApellido}"  // Esto es un placeholder. Deberías obtener el nombre del paciente de alguna forma.
        holder.tvFechaGeneracion.text = "Fecha de Creación: ${receta.fecha_create?.toString() ?: "Generación en curso"}"
        holder.tvFechaEntrega.text = "Fecha de Entrega: ${receta.fecha_entrega?.toString() ?: "No entregado aún"}"
        holder.tvEstadoReceta.text = "Estado de la Receta: ${receta.estado}"
    }
}
