package com.example.health_prescribe

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class FarmaceuticoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.farmaceutico_activity)
        val welcomeTextView: TextView = findViewById(R.id.tv_welcome_farmaceutico)
        val btnInventario: Button = findViewById(R.id.btn_inventario)
        val btn_entregar_receta: Button = findViewById(R.id.btn_entregar_receta)
        val nombre = intent.getStringExtra("nombre")
        val apellido = intent.getStringExtra("apellido")
        val farmaceuticoId = intent.getIntExtra("farmaceuticoId", -1)
        welcomeTextView.text = "Bienvenido Farmacéutico, $nombre $apellido!"
        btnInventario.setOnClickListener {
                val intent = Intent(this, SelectDrugActivity::class.java)
                intent.putExtra("idFarmaceutico", farmaceuticoId)
                startActivity(intent)

        }
        btn_entregar_receta.setOnClickListener {
            if (farmaceuticoId != -1) {
                val intent = Intent(this, FarmaceuticoEntregarRecetasActivity::class.java)
                intent.putExtra("farmaceuticoId", farmaceuticoId)  // Aquí pasas el ID
                startActivity(intent)
            } else {
                Toast.makeText(this, "No tienes permiso para acceder a esta sección.", Toast.LENGTH_SHORT).show()
            }
        }

    }
}