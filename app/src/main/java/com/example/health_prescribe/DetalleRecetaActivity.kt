package com.example.health_prescribe

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.health_prescribe.adapter.DatabaseHelper
import com.example.health_prescribe.adapter.FarmacoAdapter
import com.example.health_prescribe.model.FarmacoDisplay
import com.example.health_prescribe.model.recetas

class DetalleRecetaActivity : AppCompatActivity() {
    private lateinit var farmacoAdapter: FarmacoAdapter
    private val farmacoList = mutableListOf<FarmacoDisplay>()
    private val dbHelper = DatabaseHelper()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detalle_receta)

        // Obtener el objeto receta y el tipo de usuario del Intent
        val receta = intent.getSerializableExtra("receta") as recetas
        val esMedico = intent.getBooleanExtra("esMedico", false)
        val tvNombre = findViewById<TextView>(R.id.tv_nombre_medico)
        tvNombre.text = if (esMedico) "Nombre del paciente: " else "Nombre del paciente: "

        // Inicializar los demás TextViews
        val tvNombreValue = findViewById<TextView>(R.id.tv_nombre_medico_value)
        val tvFechaRecetaValue = findViewById<TextView>(R.id.tv_fecha_receta_value)
        val tvEstadoRecetaValue = findViewById<TextView>(R.id.tv_estado_receta_value)

        // Colocar los valores en los TextViews
        tvNombreValue.text = receta.nombreApellido
        tvFechaRecetaValue.text = receta.fecha_create?.toString() ?: "Fecha no disponible"
        tvEstadoRecetaValue.text = receta.estado
        // Inicializar RecyclerView para fármacos y su adaptador
        val farmacoRecyclerView = findViewById<RecyclerView>(R.id.recycler_view_farmacos)
        farmacoAdapter = FarmacoAdapter(farmacoList)
        farmacoRecyclerView.adapter = farmacoAdapter
        loadFarmacoData()

    }

    private fun loadFarmacoData() {
        val newFarmacos = dbHelper.fetchFarmacosFromDatabase()  // Llamada a la función
        farmacoAdapter.updateData(newFarmacos)  // Actualiza los datos en el adaptador
    }






}