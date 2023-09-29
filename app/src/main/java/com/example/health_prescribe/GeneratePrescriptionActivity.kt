package com.example.health_prescribe

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.health_prescribe.model.PacienteDetalle
import com.example.health_prescribe.model.farmacoCompleto
import com.example.health_prescribe.model.receta
import com.example.health_prescribe.model.receta_listado
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.Executor

class GeneratePrescriptionActivity : AppCompatActivity() {
    private lateinit var tv_patient_name: TextView
    private lateinit var tv_patient_lastname: TextView
    private lateinit var tv_patient_cedula: TextView
    private lateinit var tv_patient_phone: TextView
    private lateinit var tv_patient_email: TextView
    private lateinit var btnSubmitPrescription: Button
    private lateinit var btn_select_drug: Button
    private val selectedDrugs: MutableList<farmacoCompleto> = mutableListOf()
    private lateinit var spinner: Spinner
    private lateinit var recyclerView: RecyclerView
    private lateinit var selectedDrugsAdapter: RecyclerView.Adapter<*>
    var resulta = String
    var selectedPatientPositions : Int =0
    private var selectedPatients: PacienteDetalle? = null
    val fechaActual = Date()
    val formatoFecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val fechaFormateada = formatoFecha.format(fechaActual)
    private val dosisList: MutableList<String> = mutableListOf()
    private val aplicacionList: MutableList<String> = mutableListOf()
    private var fingerprintData: ByteArray? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        fun fetchPatients() {
            // Realizar la tarea de carga de pacientes en segundo plano utilizando AsyncTask
            FetchPatientsTask().execute()
        }
        val executor: Executor = ContextCompat.getMainExecutor(this)



        super.onCreate(savedInstanceState)
        setContentView(R.layout.generate_prescription) // Asegúrate de que el nombre sea el correcto

        tv_patient_name = findViewById(R.id.tv_patient_name)
        tv_patient_lastname = findViewById(R.id.tv_patient_lastname)
        tv_patient_cedula = findViewById(R.id.tv_patient_cedula)
        tv_patient_phone = findViewById(R.id.tv_patient_phone)
        tv_patient_email = findViewById(R.id.tv_patient_email)
        btnSubmitPrescription = findViewById(R.id.btn_submit_prescription)
        btn_select_drug = findViewById(R.id.btn_select_drug)
        fetchPatients()
        val medicoId = intent.getIntExtra("medicoId", 0)

        val intent = intent
        if (intent != null) {
            val firstName = intent.getStringExtra("firstName")
            val lastName = intent.getStringExtra("lastName")
            val specialty = intent.getStringExtra("specialty")
            val medicoId = intent.getIntExtra("medicoId", 0)
        } else {
            Toast.makeText(this, "El intent es nulo", Toast.LENGTH_SHORT).show()
        }
        spinner = findViewById(R.id.spinner_select_patient)
        recyclerView = findViewById(R.id.recycler_selected_drugs)
        recyclerView.layoutManager = LinearLayoutManager(this)
        selectedDrugsAdapter = SelectedDrugsAdapter(selectedDrugs)
        recyclerView.adapter = selectedDrugsAdapter
        btn_select_drug.setOnClickListener {
            val intent = Intent(this, SelectDrugActivity::class.java)
            startActivityForResult(intent, REQUEST_SELECT_DRUG)
        }
        btnSubmitPrescription.setOnClickListener {

            // Verifica que se haya seleccionado un paciente
            if (selectedPatients != null) {
                // Verifica que el campo huellaDactilar tenga datos
                if (selectedPatients?.huellaDactilar == null || selectedPatients?.huellaDactilar?.isEmpty() == true) {
                    Toast.makeText(this, "El paciente seleccionado no tiene datos de huella dactilar. Redirigiendo para captura de huella dactilar.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, JSGDActivity::class.java)
                    intent.putExtra("patientId", selectedPatients?.id_cliente)
                    startActivity(intent)
                    return@setOnClickListener
                }
                // Valida que al menos se haya seleccionado un medicamento
                if (selectedDrugs.isEmpty()) {
                    Toast.makeText(this, "Por favor, seleccione al menos un medicamento", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                // Valida que se haya ingresado una cantidad y descripción para cada medicamento
                val dosisEditText = findViewById<EditText>(R.id.et_quantity)
                val aplicacionEditText = findViewById<EditText>(R.id.et_prescription)
                for (selectedDrug in selectedDrugs) {
                    val dosis = dosisEditText.text.toString().trim()
                    val aplicacion = aplicacionEditText.text.toString().trim()

                    if (dosis.isEmpty() || aplicacion.isEmpty()) {
                        Toast.makeText(this, "Por favor, complete la cantidad y descripción para cada medicamento", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }

                // Crea una instancia de receta con los datos necesarios
                val receta = receta(
                    id_medico = medicoId,
                    id_paciente = selectedPatients?.id_cliente ?: 0,
                    estado = "Pendiente",
                    codigo_receta = (10000000..99999999).random().toString(),
                    create_asistido = false,
                    fecha_create = fechaFormateada
                )

                // Convierte la lista inmutable en una lista mutable
                val detallesRecetaMutable: MutableList<receta_listado> = selectedDrugs.map { selectedDrug ->
                    receta_listado(
                        id_farmaco = selectedDrug.id_farmaco,
                        dosis = dosisEditText.text.toString().toIntOrNull() ?: 0,
                        aplicacion = aplicacionEditText.text.toString()
                    )
                }.toMutableList()

                // Ejecuta la tarea en segundo plano para guardar la receta
                SaveRecetaTask(receta, detallesRecetaMutable).execute()

                // Limpia completamente la lista de medicamentos seleccionados
                selectedDrugs.clear()
                selectedDrugsAdapter.notifyDataSetChanged()

                // Limpia los campos después de guardar la receta
                dosisEditText.text.clear()
                aplicacionEditText.text.clear()
            } else {
                // Maneja el caso en el que no se haya seleccionado un paciente
                Toast.makeText(this, "Por favor, seleccione un paciente", Toast.LENGTH_SHORT).show()
            }
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_DRUG && resultCode == RESULT_OK) {
            val selectedDrug = data?.getSerializableExtra("selectedDrug") as? farmacoCompleto
            if (selectedDrug != null) {
                addSelectedDrug(selectedDrug)
            }
        }

    }

    private inner class FetchPatientsTask : AsyncTask<Void, Void, List<PacienteDetalle>>() {

        override fun doInBackground(vararg params: Void?): List<PacienteDetalle> {
            return DatabaseConnection.fetchPatientsFromDB()
        }

        override fun onPostExecute(result: List<PacienteDetalle>) {
            val adapter = ArrayAdapter(
                this@GeneratePrescriptionActivity,
                android.R.layout.simple_spinner_item,
                result.map { "${it.nombre} ${it.apellido}" }
            )
            spinner.adapter = adapter
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedPatients = result[position]
                    tv_patient_name.text = " ${selectedPatients?.nombre}"
                    tv_patient_lastname.text = " ${selectedPatients?.apellido}"
                    tv_patient_cedula.text = " ${selectedPatients?.cedula}"
                    tv_patient_phone.text = " 0${selectedPatients?.telefono}"
                    tv_patient_email.text = " ${selectedPatients?.correo}"
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        }
    }

    private fun addSelectedDrug(selectedDrug: farmacoCompleto) {
        selectedDrugs.add(selectedDrug)
        selectedDrugsAdapter.notifyDataSetChanged()
    }

    inner class SelectedDrugsAdapter(private val drugs: List<farmacoCompleto>) :
        RecyclerView.Adapter<SelectedDrugsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tv_selected_drug_name)
            val tvConcentration: TextView = view.findViewById(R.id.tv_selected_drug_concentration)
            val tvProvider: TextView = view.findViewById(R.id.tv_selected_drug_provider)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.selected_drug_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val selectedDrug = drugs[position]
            holder.tvName.text = " ${selectedDrug.nombre_generico}"
            holder.tvConcentration.text = " ${selectedDrug.concentracion}"
            holder.tvProvider.text = " ${selectedDrug.proveedor.nombre_proveedor}"
        }

        override fun getItemCount(): Int {
            return drugs.size
        }
    }

    companion object {
        const val REQUEST_SELECT_DRUG = 1
    }

    // AsyncTask para guardar la receta en segundo plano
    private inner class SaveRecetaTask(
        private val receta: receta,
        private val detallesReceta: MutableList<receta_listado>
    ) : AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg params: Void?): Boolean {
            // Llama a la función saveReceta para guardar la receta en la base de datos
            return DatabaseConnection.saveReceta(receta, detallesReceta)
        }

        override fun onPostExecute(result: Boolean) {
            if (result) {
                // Si la receta se guarda con éxito, muestra un mensaje de éxito

                Toast.makeText(this@GeneratePrescriptionActivity, "Receta guardada con éxito", Toast.LENGTH_SHORT).show()
            } else {
                // Si hay un error al guardar la receta, muestra un mensaje de error
                Toast.makeText(this@GeneratePrescriptionActivity, "Error al guardar la receta", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
