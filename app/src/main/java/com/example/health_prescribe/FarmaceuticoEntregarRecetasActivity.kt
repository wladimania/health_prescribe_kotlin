package com.example.health_prescribe

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.health_prescribe.adapter.FarmacoAdapter
import com.example.health_prescribe.model.FarmacoDisplay
import com.example.health_prescribe.model.farmaco
import com.example.health_prescribe.model.receta_listado
import kotlinx.coroutines.*

class FarmaceuticoEntregarRecetasActivity : AppCompatActivity() {
    private val farmacosList = mutableListOf<receta_listado>()
    private lateinit var farmacosAdapter: FarmacoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_farmaceutico_entregar_recetas)

        val et_buscar_receta: EditText = findViewById(R.id.et_buscar_receta)
        val rvFarmacos: RecyclerView = findViewById(R.id.rv_farmacos)

        // Inicializa el adaptador aquí
        farmacosAdapter = FarmacoAdapter(farmacoDisplayList)


        // Configurar el RecyclerView
        rvFarmacos.layoutManager = LinearLayoutManager(this)
        rvFarmacos.adapter = farmacosAdapter

        et_buscar_receta.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                buscarReceta(s.toString())
            }

            override fun afterTextChanged(s: Editable) {}
        })

        // Puedes iniciar una búsqueda por defecto al inicio si lo deseas
        buscarReceta("CODIGO_EJEMPLO")
    }

    private fun transformToDisplay(list: List<receta_listado>, farmacos: List<farmaco>): List<FarmacoDisplay> {
        return list.map { item ->
            val farmacoInfo = farmacos.find { it.id_farmaco == item.id_farmaco }
            FarmacoDisplay(
                id_farmaco = item.id_farmaco,
                nombre_generico = farmacoInfo?.nombre_generico ?: "Desconocido",
                cantidad = item.cantidad ?: 0,
                dosis = item.dosis,
                aplicacion = item.aplicacion
            )
        }.toList() // Agrega .toList() para crear una nueva lista
    }



    private val farmacoDisplayList = mutableListOf<FarmacoDisplay>()


    fun buscarReceta(codigoReceta: String) {
        GlobalScope.launch(Dispatchers.Main) {
            val receta = withContext(Dispatchers.IO) {
                DatabaseConnection.getRecetaByCodigo(codigoReceta)
            }

            val tv_paciente_nombre: TextView = findViewById(R.id.tv_paciente_nombre)
            val tv_medico_nombre: TextView = findViewById(R.id.tv_medico_nombre)

            if (receta != null) {
                val paciente = withContext(Dispatchers.IO) {
                    DatabaseConnection.getPatientById(receta.id_paciente)
                }
                tv_paciente_nombre.text = "Nombre del Paciente: ${paciente?.nombre} ${paciente?.apellido}"

                val medico = withContext(Dispatchers.IO) {
                    DatabaseConnection.getMedicoById(receta.id_medico)
                }
                tv_medico_nombre.text = "Nombre del Médico: ${medico?.nombre} ${medico?.apellido}"

                val farmacos = withContext(Dispatchers.IO) {
                    DatabaseConnection.getAllFarmacos() // Asumiendo que tienes una función que obtiene todos los fármacos
                }

                val listado = withContext(Dispatchers.IO) {
                    DatabaseConnection.getFarmacosForReceta(receta.id_receta!!)
                }


                farmacoDisplayList.clear()
                farmacoDisplayList.addAll(transformToDisplay(listado, farmacos))
                farmacosAdapter.notifyDataSetChanged()


            }
        }
    }


}
