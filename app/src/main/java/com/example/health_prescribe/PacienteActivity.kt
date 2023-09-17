package com.example.health_prescribe

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class PacienteActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val firstName = intent.getStringExtra("firstName")
        val lastName = intent.getStringExtra("lastName")

        val tvPatientName = findViewById<TextView>(R.id.tv_patient_name)
        tvPatientName.text = "$firstName $lastName"

        setContentView(R.layout.paciente_activity)
    }
}