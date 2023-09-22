package com.example.health_prescribe

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class PacienteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.paciente_activity)
        val idPersona = intent.getIntExtra("id_persona", -1)
        val nombre = intent.getStringExtra("nombre")
        val apellido = intent.getStringExtra("apellido")


        val tvPatientName = findViewById<TextView>(R.id.tv_patient_name)
        tvPatientName.text = "$nombre $apellido"

        val btnViewPrescriptions = findViewById<Button>(R.id.btn_view_prescriptions_patient)
        btnViewPrescriptions.setOnClickListener {
            val intent = Intent(this, ListadoRecetasMedicoActivity::class.java)
            intent.putExtra("id_persona", idPersona)
            startActivity(intent)
        }

    }
}