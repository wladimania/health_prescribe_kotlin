package com.example.health_prescribe

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.example.health_prescribe.model.farmacoCompleto

class SelectDrugActivity : AppCompatActivity() {
    private val selectedDrugs: MutableList<farmacoCompleto> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.selectdrugactivity)

        FetchDrugsTask(this).execute()
    }

    fun displayDrugs(drugs: List<farmacoCompleto>) {
        val drugsContainer: LinearLayout = findViewById(R.id.drugsContainer)

        for (drug in drugs) {
            val drugView = LayoutInflater.from(this).inflate(R.layout.drug_item, null)

            val drugNameView = drugView.findViewById<TextView>(R.id.tv_drug_name)
            drugNameView.text = "Nombre del medicamento: ${drug.nombre_generico}"

            val drugConcentrationView = drugView.findViewById<TextView>(R.id.tv_drug_concentration)
            drugConcentrationView.text = "Concentración del medicamento: ${drug.concentracion}"

            val drugLabView = drugView.findViewById<TextView>(R.id.tv_drug_lab)
            drugLabView.text = "Cantidad en inventario: ${drug.inventario}"

            val drugProviderView = drugView.findViewById<TextView>(R.id.tv_drug_provider)
            drugProviderView.text = "Nombre del proveedor: ${drug.proveedor.nombre_proveedor}"

            drugView.setOnClickListener {
                // Cuando se hace clic en la CardView del medicamento, lo agregamos a la lista de seleccionados
                selectedDrugs.add(drug)
                // Devolvemos el medicamento seleccionado a GeneratePrescriptionActivity
                val intent = Intent()
                intent.putExtra("selectedDrug", drug)
                setResult(RESULT_OK, intent)
                finish()
            }

            drugsContainer.addView(drugView)
        }
    }
}
