package com.example.health_prescribe

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.health_prescribe.DatabaseConnection.obtenerIdClientePorCodigoRecetaAsync
import com.example.health_prescribe.adapter.FarmacoAdapter
import com.example.health_prescribe.model.FarmacoDisplay
import com.example.health_prescribe.model.farmaco
import com.example.health_prescribe.model.receta_listado
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FarmaceuticoEntregarRecetasActivity : AppCompatActivity() {
    private val TAG = "FarmaceuticoEntregarRecetasActivity"
    private val farmacosList = mutableListOf<receta_listado>()
    private lateinit var farmacosAdapter: FarmacoAdapter
    private var farmaceuticoId: Int = -1 // Declarar la variable aquí
    private var id_cliente: Int =-1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_farmaceutico_entregar_recetas)

        farmaceuticoId = intent.getIntExtra("farmaceuticoId", -1) // Inicializar aquí

        if (farmaceuticoId == -1) {
            // El ID no se pasó correctamente. Posiblemente cerrar la actividad y volver a la anterior.
            finish()
            return
        }

        val et_buscar_receta: EditText = findViewById(R.id.et_buscar_receta)
        val rvFarmacos: RecyclerView = findViewById(R.id.rv_farmacos)

        // Inicializar el adaptador aquí
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

        val btn_entregar_receta: Button = findViewById(R.id.btn_entregar_receta)
        btn_entregar_receta.setOnClickListener {

            val codigoReceta = et_buscar_receta.text.toString()

            // Consultar la base de datos para obtener el ID del cliente
            obtenerIdClientePorCodigoRecetaAsync(codigoReceta) { clienteId ->
                // Maneja el clienteId aquí en el hilo principal.
                // Por ejemplo, puedes mostrarlo en un TextView o realizar otras acciones.
                runOnUiThread {
                    // Actualiza la interfaz de usuario con el clienteId.
                    id_cliente = clienteId

                    if (id_cliente != -1) {
                        // Crear un Intent para iniciar la actividad ValidarReceta
                        val intent = Intent(this, ValidarReceta::class.java)
                        intent.putExtra("farmaceuticoId", farmaceuticoId)
                        intent.putExtra("clienteId", id_cliente)
                        intent.putExtra("codigoReceta", codigoReceta)
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Cliente no encontrado", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }




    }

    private fun transformToDisplay(
        list: List<receta_listado>,
        farmacos: List<farmaco>
    ): List<FarmacoDisplay> {
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

    fun entregarReceta() {
        val et_buscar_receta: EditText = findViewById(R.id.et_buscar_receta)



        // Iniciar la actividad
        startActivity(intent)

    }

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
