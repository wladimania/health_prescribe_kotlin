package com.example.health_prescribe

import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.health_prescribe.model.recetas

class ListadoRecetasMedicoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_listado_recetas_medico)

        val medicoId = intent.getIntExtra("medicoId", -1)

        FetchRecetasTask(medicoId) { recetas ->
            val recyclerView = findViewById<RecyclerView>(R.id.recycler_recetas_medico)
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = RecetasMedicoAdapter(recetas)
            Log.d("ListadoRecetasMedico", "Número de recetas en la actividad: ${recetas.size}")
        }.execute()

    }

    private fun fetchRecetasFromDB(medicoId: Int): List<recetas> {
        val recetasList = mutableListOf<recetas>()
        val connection = DatabaseConnection.getConnection()
        val statement = connection?.createStatement()
        val resultSet = statement?.executeQuery("""
        SELECT 
    r.id_receta, 
    r.id_medico, 
    r.id_paciente, 
    r.estado, 
    r.id_farmaceutico, 
    r.codigo_receta, 
    r.create_asistido, 
    r.fecha_create, 
    r.fecha_entrega,
    CONCAT(p.nombre, ' ', p.apellido) AS nombreApellido
FROM 
    receta r 
JOIN 
    cliente c ON r.id_paciente = c.id_cliente 
JOIN 
    persona p ON c.id_persona = p.id_persona 
WHERE 
    r.id_medico = $medicoId
    """)

        if (resultSet != null) {
            while (resultSet.next()) {
                val recetaItem = recetas(
                    resultSet.getInt("id_receta"),
                    resultSet.getInt("id_medico"),
                    resultSet.getInt("id_paciente"),
                    resultSet.getString("estado"),
                    resultSet.getInt("id_farmaceutico"),
                    resultSet.getString("codigo_receta"),
                    resultSet.getBoolean("create_asistido"),
                    resultSet.getDate("fecha_create"),
                    resultSet.getDate("fecha_entrega"),
                    resultSet.getString("nombreApellido")
                )
                recetasList.add(recetaItem)
            }
        }
        connection?.close()
        return recetasList
    }



    private inner class FetchRecetasTask(private val medicoId: Int, private val onResult: (List<recetas>) -> Unit) : AsyncTask<Void, Void, List<recetas>>() {
        override fun doInBackground(vararg params: Void?): List<recetas> {
            return fetchRecetasFromDB(medicoId)
        }
        override fun onPostExecute(result: List<recetas>) {
            onResult(result)
        }
    }
}
