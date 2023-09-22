package com.example.health_prescribe

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.health_prescribe.adapter.DatabaseHelper
import com.example.health_prescribe.model.farmacoCompleto
import kotlinx.coroutines.*

class FarmaceuticoInventarioActivity : AppCompatActivity() {
    private val scope = CoroutineScope(Dispatchers.Main)
    private lateinit var drug: farmacoCompleto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_farmaceutico_inventario)
        val dbHelper = DatabaseHelper()


        // Recibe el medicamento
        drug = intent.getSerializableExtra("selectedDrug") as farmacoCompleto
        dbHelper.updateDrug(drug)
        // Configura los campos con los datos del medicamento
        val etNombreMedicamento: EditText = findViewById(R.id.et_nombre_medicamento)
        etNombreMedicamento.setText(drug.nombre_generico)

        val etCantidadMedicamento: EditText = findViewById(R.id.et_cantidad_medicamento)
        etCantidadMedicamento.setText(drug.inventario.toString())  // Suponiendo que inventario es un Int, se convierte a String

        val etConcentracion: EditText = findViewById(R.id.et_concentracion)
        etConcentracion.setText(drug.concentracion)

        // Configura el Spinner con el nombre del proveedor
        val spinnerProveedor: Spinner = findViewById(R.id.spinner_proveedor)
        val providersList = listOf(drug.proveedor.nombre_proveedor)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, providersList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerProveedor.adapter = adapter

// ... dentro de tu método onCreate() ...

        val btnUpdate: Button = findViewById(R.id.btn_update)
        btnUpdate.setOnClickListener {
            // Obteniendo los nuevos valores de los campos
            val nuevoNombre = etNombreMedicamento.text.toString()
            val nuevaCantidad = etCantidadMedicamento.text.toString().toInt()
            val nuevaConcentracion = etConcentracion.text.toString()
            val proveedorSeleccionado = spinnerProveedor.selectedItem.toString()

            // Actualizando el objeto drug con los nuevos valores
            drug.nombre_generico = nuevoNombre
            drug.inventario = nuevaCantidad
            drug.concentracion = nuevaConcentracion
            drug.proveedor.nombre_proveedor = proveedorSeleccionado

            scope.launch {
                val wasUpdated = withContext(Dispatchers.IO) {
                    dbHelper.updateDrug(drug)
                }

                if (wasUpdated) {
                    Toast.makeText(this@FarmaceuticoInventarioActivity, "Medicamento actualizado con éxito", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK) // Configura el resultado para informar que hubo una actualización
                    finish()
                } else {
                    Toast.makeText(this@FarmaceuticoInventarioActivity, "Error al actualizar el medicamento", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}