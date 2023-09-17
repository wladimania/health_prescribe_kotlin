package com.example.health_prescribe

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MedicoActivity : AppCompatActivity() {

    private lateinit var tvDoctorName: TextView
    private lateinit var tvDoctorSpecialty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.medico_activity)

        // Inicializa las vistas
        tvDoctorName = findViewById(R.id.tv_doctor_name)
        tvDoctorSpecialty = findViewById(R.id.tv_doctor_specialty)

        // Recupera los datos pasados desde LoginActivity
        val firstName = intent.getStringExtra("firstName")
        val lastName = intent.getStringExtra("lastName")
        val specialty = intent.getStringExtra("specialty")
        val medicoId = intent.getIntExtra("medicoId", -1)

        // Configura los TextViews con los datos recuperados
        tvDoctorName.text = "Dr. $firstName $lastName"
        tvDoctorSpecialty.text = "Especialidad: $specialty"

        // Botón para generar receta
        val btnGeneratePrescription = findViewById<Button>(R.id.btn_generate_prescription)
        btnGeneratePrescription.setOnClickListener {
            val intent = Intent(this, GeneratePrescriptionActivity::class.java)
            startActivity(intent)
        }

        // Botón para visualizar mis recetas
        val btnViewPrescriptions = findViewById<Button>(R.id.btn_view_prescriptions)
        btnViewPrescriptions.setOnClickListener {
            val intent = Intent(this, ListadoRecetasMedicoActivity::class.java)
            intent.putExtra("medicoId", medicoId)
            startActivity(intent)

        }
    }
}
