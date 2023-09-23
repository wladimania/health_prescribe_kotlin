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
        val id_persona = intent.getIntExtra("id_persona", -1)

        FetchRecetasTask(medicoId, id_persona) { recetas ->
            val recyclerView = findViewById<RecyclerView>(R.id.recycler_recetas_medico)
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = RecetasMedicoAdapter(this, recetas, medicoId, id_persona)  // Añade los IDs aquí
            Log.d("ListadoRecetasMedico", "Número de recetas en la actividad: ${recetas.size}")
        }.execute()
    }

    private fun fetchRecetasFromDB(medicoId: Int, pacienteId: Int): List<recetas> {
        val recetasList = mutableListOf<recetas>()
        val connection = DatabaseConnection.getConnection()
        val statement = connection?.createStatement()

        val query: String = if (medicoId != -1) {
            """
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
        AND r.fecha_create IS NOT NULL
ORDER BY
    r.fecha_create DESC;
    """
        } else if (pacienteId != -1) {
            """
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
    CONCAT(p.nombre, ' ', p.apellido) AS nombreApellido,
    p.id_persona
FROM
    receta r
JOIN
    cliente c ON r.id_paciente = c.id_cliente
JOIN
    persona p ON c.id_persona = p.id_persona
WHERE
    p.id_persona = $pacienteId
    AND r.fecha_create IS NOT NULL
ORDER BY
    r.fecha_create DESC;
    """
        } else {
            throw IllegalArgumentException("No se proporcionó un ID válido para médico o paciente.")
        }


        val resultSet = statement?.executeQuery(query)

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

    private inner class FetchRecetasTask(
        private val medicoId: Int,
        private val pacienteId: Int,
        private val onResult: (List<recetas>) -> Unit
    ) : AsyncTask<Void, Void, List<recetas>>() {

        override fun doInBackground(vararg params: Void?): List<recetas> {
            return fetchRecetasFromDB(medicoId, pacienteId)
        }

        override fun onPostExecute(result: List<recetas>) {
            onResult(result)
        }
    }
}
