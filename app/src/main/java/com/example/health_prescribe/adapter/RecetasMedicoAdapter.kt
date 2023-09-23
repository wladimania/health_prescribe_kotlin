package com.example.health_prescribe

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.health_prescribe.model.recetas


class RecetasMedicoAdapter( private val context: Context,
                            private val recetasList: List<recetas>,
                            private val medicoId: Int,
                            private val id_persona: Int
) : RecyclerView.Adapter<RecetasMedicoAdapter.RecetaViewHolder>() {

    inner class RecetaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombrePaciente: TextView = itemView.findViewById(R.id.tv_nombre_apellidos_paciente)
        val tvFechaGeneracion: TextView = itemView.findViewById(R.id.tv_fecha_generacion)
        val tvFechaEntrega: TextView = itemView.findViewById(R.id.tv_fecha_entrega)
        val tvEstadoReceta: TextView = itemView.findViewById(R.id.tv_estado_receta)
        val tvCodigoReceta: TextView = itemView.findViewById(R.id.tv_codigo_receta)
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
        holder.tvNombrePaciente.text = " ${receta.nombreApellido}"  // Esto es un placeholder. Deberías obtener el nombre del paciente de alguna forma.
        holder.tvFechaGeneracion.text = " ${receta.fecha_create?.toString() ?: "Generación en curso"}"
        holder.tvFechaEntrega.text = " ${receta.fecha_entrega?.toString() ?: "No entregado aún"}"
        holder.tvEstadoReceta.text = " ${receta.estado}"
        holder.tvCodigoReceta.text = " ${receta.codigo_receta}"
        // Aquí es donde agregas el OnClickListener
        holder.itemView.setOnClickListener {
            val intent = Intent(context, DetalleRecetaActivity::class.java)
            intent.putExtra("receta", receta)
            intent.putExtra("esMedico", medicoId != -1)  // Determina si es médico o paciente
            context.startActivity(intent)
        }
    }
}
