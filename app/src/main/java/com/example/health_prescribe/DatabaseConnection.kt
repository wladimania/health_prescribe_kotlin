package com.example.health_prescribe

import android.util.Log
import com.example.health_prescribe.model.PacienteDetalle
import com.example.health_prescribe.model.farmacoCompleto
import com.example.health_prescribe.model.proveedor
import com.example.health_prescribe.model.receta
import com.example.health_prescribe.model.receta_listado
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

object DatabaseConnection {

    private const val url = "jdbc:postgresql://10.0.2.2:5432/farmaceutica"
    private const val user = "postgres"
    private const val password = "123456789"

    fun getConnection(): Connection? {
        return try {
            DriverManager.getConnection(url, user, password)
        } catch (e: Exception) {
            Log.e("DBConnectionError", "Error al obtener la conexión", e)
            null
        }
    }

    fun fetchPatientsFromDB(): List<PacienteDetalle> {
        val patients = mutableListOf<PacienteDetalle>()
        val connection = getConnection()
        val statement = connection?.createStatement()
        val resultSet =
            statement?.executeQuery("SELECT * FROM cliente INNER JOIN persona ON cliente.id_persona = persona.id_persona WHERE cliente.habilitado = TRUE")

        if (resultSet != null) {
            while (resultSet.next()) {
                val huellaDactilarBytes = resultSet.getBytes("huellaDactilar")
                val huellaDactilar = if (huellaDactilarBytes != null) huellaDactilarBytes else byteArrayOf() // Valor predeterminado: array de bytes vacío
                val patient = PacienteDetalle(
                    resultSet.getInt("id_cliente") ?: 0,
                    resultSet.getInt("id_persona") ?: 0,
                    resultSet.getString("nombre") ?: "",
                    resultSet.getString("apellido") ?: "",
                    resultSet.getString("cedula") ?: "",
                    resultSet.getString("telefono") ?: "",
                    resultSet.getBoolean("habilitado") ?: false,
                    resultSet.getString("correo") ?: "",
                    huellaDactilar // Se usa el valor predeterminado si huellaDactilarBytes es nulo
                )
                patients.add(patient)
            }
        }

        connection?.close()
        return patients
    }
    fun fetchDrugsFromDB(): List<farmacoCompleto> {
        val drugs = mutableListOf<farmacoCompleto>()
        val connection = getConnection()
        val statement = connection?.createStatement()

        // JOIN entre las tablas farmaco y proveedor
        val resultSet = statement?.executeQuery("""
        SELECT * 
        FROM farmaco 
        INNER JOIN proveedor ON farmaco.id_proveedor = proveedor.id_proveedor
    """)

        if (resultSet != null) {
            while (resultSet.next()) {
                val prov = proveedor(
                    resultSet.getInt("id_proveedor")?: 0,
                    resultSet.getString("nombre_proveedor")?: ""
                )

                val drug = farmacoCompleto(
                    resultSet.getInt("id_farmaco")?: 0,
                    resultSet.getString("nombre_generico")?: "",
                    resultSet.getString("forma_farmaceutica")?: "",
                    resultSet.getInt("inventario")?: 0,
                    resultSet.getString("concentracion")?: "",
                    prov,
                    resultSet.getString("bar_code")?: ""
                )

                drugs.add(drug)
            }
        }

        connection?.close()
        return drugs
    }
    fun saveReceta(receta: receta, detallesReceta: MutableList<receta_listado>): Boolean {
        val connection = getConnection()
        val statement = connection?.createStatement()

        // Insertar la receta en la base de datos
        val recetaInsertQuery = """
        INSERT INTO receta (id_medico, id_paciente, estado, codigo_receta, fecha_create)
        VALUES (${receta.id_medico}, ${receta.id_paciente}, '${receta.estado}', '${receta.codigo_receta}', '${receta.fecha_create}')
        RETURNING id_receta
    """.trimIndent()

        val recetaResultSet = statement?.executeQuery(recetaInsertQuery)
        var nuevoRecetaId: Int

        if (recetaResultSet?.next() == true) {
            nuevoRecetaId = recetaResultSet.getInt("id_receta")
        } else {
            // Manejo de error si no se pudo insertar la receta
            connection?.close()
            return false
        }

        // Insertar los detalles de la receta en la base de datos
        for (detalle in detallesReceta) {
            val detalleInsertQuery = """
        INSERT INTO receta_listado (id_receta, id_farmaco, dosis, aplicacion)
        VALUES ($nuevoRecetaId, ${detalle.id_farmaco}, ${detalle.dosis}, '${detalle.aplicacion}')
    """
            val detalleInsertResult = statement?.executeUpdate(detalleInsertQuery)

            if (detalleInsertResult != 1) {
                // Manejo de error si no se pudo insertar un detalle
                connection?.close()
                return false
            }
        }

        connection?.close()
        return true
    }

    fun getRecetaId(codigoReceta: String): Int {
        val connection = getConnection()
        val statement = connection?.createStatement()

        val query = "SELECT id_receta FROM receta WHERE codigo_receta = '$codigoReceta'"
        val resultSet = statement?.executeQuery(query)

        var recetaId = 0

        if (resultSet?.next() == true) {
            recetaId = resultSet.getInt("id_receta")
        }

        connection?.close()
        return recetaId
    }

    fun saveDetallesReceta(detallesReceta: List<receta_listado>): Boolean {
        val connection = getConnection()
        val statement = connection?.createStatement()

        for (detalle in detallesReceta) {
            val query = """
            INSERT INTO receta_listado (id_receta, id_farmaco, dosis, aplicacion)
            VALUES (${detalle.id_receta}, ${detalle.id_farmaco}, ${detalle.dosis}, '${detalle.aplicacion}')
        """
            val result = statement?.executeUpdate(query)

            if (result != 1) {
                connection?.close()
                return false
            }
        }

        connection?.close()
        return true
    }


}



