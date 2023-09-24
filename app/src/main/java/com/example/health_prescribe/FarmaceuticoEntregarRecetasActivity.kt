package com.example.health_prescribe

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.health_prescribe.adapter.FarmacoAdapter
import com.example.health_prescribe.model.FarmacoDisplay
import com.example.health_prescribe.model.farmaco
import com.example.health_prescribe.model.receta_listado
import kotlinx.coroutines.*
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor


class FarmaceuticoEntregarRecetasActivity : AppCompatActivity() {
    private val farmacosList = mutableListOf<receta_listado>()
    private lateinit var farmacosAdapter: FarmacoAdapter
    private var farmaceuticoId: Int = -1 // Declarar la variable aquí

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_farmaceutico_entregar_recetas)

        farmaceuticoId = intent.getIntExtra("farmaceuticoId", -1) // Inicializar aquí

        if(farmaceuticoId == -1) {
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
            entregarReceta()
        }

        /**
         *
         */
        // Dentro de un método onClick o similar
        mostrarDialogoAutenticacionBiometrica()
    }

    fun mostrarDialogoAutenticacionBiometrica() {
        val executor: Executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Manejar errores de autenticación
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // Autenticación exitosa, realiza las acciones necesarias aquí
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Autenticación fallida
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticación Biométrica")
            .setSubtitle("Utiliza tu huella digital para autenticarte")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
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
    fun entregarReceta() {
        val et_buscar_receta: EditText = findViewById(R.id.et_buscar_receta)
        val codigoReceta = et_buscar_receta.text.toString()

        GlobalScope.launch(Dispatchers.Main) {
            val receta = withContext(Dispatchers.IO) {
                DatabaseConnection.getRecetaByCodigo(codigoReceta)
            }

            if (receta != null) {
                val actualizado = withContext(Dispatchers.IO) {
                    DatabaseConnection.updateRecetaEstadoYFarmaceutico(receta.id_receta!!, "Entregada", farmaceuticoId)
                }

                if (actualizado) {
                    Toast.makeText(this@FarmaceuticoEntregarRecetasActivity, "Receta entregada con éxito", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@FarmaceuticoEntregarRecetasActivity, "Error al entregar la receta", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@FarmaceuticoEntregarRecetasActivity, "Receta no encontrada", Toast.LENGTH_SHORT).show()
            }
        }
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
