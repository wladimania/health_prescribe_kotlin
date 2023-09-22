package com.example.health_prescribe

import android.util.Log
import com.example.health_prescribe.model.PacienteDetalle
import com.example.health_prescribe.model.farmaceutico
import com.example.health_prescribe.model.farmaco
import com.example.health_prescribe.model.farmacoCompleto
import com.example.health_prescribe.model.medico
import com.example.health_prescribe.model.proveedor
import com.example.health_prescribe.model.receta
import com.example.health_prescribe.model.receta_listado
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement

object DatabaseConnection {

    private const val url = "jdbc:postgresql://ec2-34-202-53-101.compute-1.amazonaws.com:5432/d39ljtg5nn3bvr?sslmode=require"
    private const val user = "dnspdplhkzkedv"
    private const val password = "4e6a1ef6e41639e819361f97326d377bf9c7ca30e585325e15585adecdc5bc3f"

    init {
        try {
            // Cargar el driver de PostgreSQL
            Class.forName("org.postgresql.Driver")
        } catch (e: Exception) {
            Log.e("DBConnectionError", "Error al cargar el driver", e)
        }
    }

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
        INSERT INTO receta_listado (id_receta, id_farmaco, cantidad, aplicacion)
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
            INSERT INTO receta_listado (id_receta, id_farmaco, cantidad, aplicacion)
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
// Extendiendo la clase DatabaseConnection

    fun getRecetaByCodigo(codigoReceta: String): receta? {
        val connection = getConnection()
        val statement = connection?.createStatement()

        val query = "SELECT * FROM receta WHERE codigo_receta = '$codigoReceta'"
        val resultSet = statement?.executeQuery(query)

        var rec: receta? = null

        if (resultSet?.next() == true) {
            rec = receta(
                resultSet.getInt("id_receta"),
                resultSet.getInt("id_medico"),
                resultSet.getInt("id_paciente"),
                resultSet.getString("estado"),
                resultSet.getInt("id_farmaceutico"),
                resultSet.getString("codigo_receta"),
                resultSet.getBoolean("create_asistido"),
                resultSet.getString("fecha_create"),
                resultSet.getDate("fecha_entrega")
            )
        }

        connection?.close()
        return rec
    }

    data class Medico(
        val idMedico: Int,
        val nombre: String,
        val apellido: String,
        val especializacion: String,
        val habilitado: Boolean
    )

    fun getMedicoById(idMedico: Int): Medico? {
        val connection = getConnection()
        val statement = connection?.createStatement()

        val query = """
        SELECT m.id_medico, p.nombre, p.apellido, m.especializacion, m.habilitado 
        FROM medico m 
        INNER JOIN persona p ON m.id_persona = p.id_persona 
        WHERE m.id_medico = $idMedico
    """
        val resultSet = statement?.executeQuery(query)

        var med: Medico? = null

        if (resultSet?.next() == true) {
            med = Medico(
                resultSet.getInt("id_medico"),
                resultSet.getString("nombre"),
                resultSet.getString("apellido"),
                resultSet.getString("especializacion"),
                resultSet.getBoolean("habilitado")
            )
        }

        connection?.close()
        return med
    }


    fun getFarmacosForReceta(idReceta: Int): List<receta_listado> {
        val connection = getConnection()
        val statement = connection?.createStatement()

        val query = """
    SELECT * 
    FROM receta_listado 
    WHERE id_receta = $idReceta
    """
        val resultSet = statement?.executeQuery(query)

        val farmacosList = mutableListOf<receta_listado>()

        while (resultSet?.next() == true) {
            val recListado = receta_listado(
                resultSet.getInt("id_receta"),
                resultSet.getInt("id_farmaco"),
                resultSet.getInt("cantidad"),
                resultSet.getInt("dosis"),
                resultSet.getInt("id_receta_listado"),
                resultSet.getString("aplicacion")
            )

            farmacosList.add(recListado)
        }

        connection?.close()
        return farmacosList
    }
    fun getPatientById(idCliente: Int): PacienteDetalle? {
        val connection = getConnection()
        val statement = connection?.createStatement()

        val query = "SELECT * FROM cliente INNER JOIN persona ON cliente.id_persona = persona.id_persona WHERE cliente.id_cliente = $idCliente"
        val resultSet = statement?.executeQuery(query)

        var paciente: PacienteDetalle? = null

        if (resultSet?.next() == true) {
            val huellaDactilarBytes = resultSet.getBytes("huellaDactilar")
            val huellaDactilar = if (huellaDactilarBytes != null) huellaDactilarBytes else byteArrayOf() // Valor predeterminado: array de bytes vacío

            paciente = PacienteDetalle(
                resultSet.getInt("id_cliente") ?: 0,
                resultSet.getInt("id_persona") ?: 0,
                resultSet.getString("nombre") ?: "",
                resultSet.getString("apellido") ?: "",
                resultSet.getString("cedula") ?: "",
                resultSet.getString("telefono") ?: "",
                resultSet.getBoolean("habilitado") ?: false,
                resultSet.getString("correo") ?: "",
                huellaDactilar
            )
        }

        connection?.close()
        return paciente
    }
    fun getFarmacoById(idFarmaco: Int): farmaco? {
        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null

        try {
            connection = getConnection()
            statement = connection?.createStatement()

            val query = "SELECT * FROM farmaco WHERE id_farmaco = $idFarmaco"
            resultSet = statement?.executeQuery(query)

            if (resultSet?.next() == true) {
                return farmaco(
                    resultSet.getInt("id_farmaco"),
                    resultSet.getString("nombre_generico") ?: "",
                    resultSet.getString("forma_farmaceutica") ?: "",
                    resultSet.getInt("inventario"),
                    resultSet.getString("concentracion") ?: "",
                    resultSet.getInt("id_proveedor"),
                    resultSet.getString("bar_code") ?: ""
                )
            }
        } catch (ex: SQLException) {
            // Aquí puedes manejar o registrar el error si lo deseas
            ex.printStackTrace()
        } finally {
            // Asegúrate de cerrar los recursos en el bloque finally
            resultSet?.close()
            statement?.close()
            connection?.close()
        }

        return null
    }

    fun getAllFarmacos(): List<farmaco> {
        val farmacos = mutableListOf<farmaco>()
        val connection = getConnection()
        val statement = connection?.createStatement()

        val resultSet = statement?.executeQuery("SELECT * FROM farmaco")

        if (resultSet != null) {
            while (resultSet.next()) {
                val drug = farmaco(
                    resultSet.getInt("id_farmaco") ?: 0,
                    resultSet.getString("nombre_generico") ?: "",
                    resultSet.getString("forma_farmaceutica") ?: "",
                    resultSet.getInt("inventario") ?: 0,
                    resultSet.getString("concentracion") ?: "",
                    resultSet.getInt("id_proveedor") ?: 0,
                    resultSet.getString("bar_code") ?: ""
                )
                farmacos.add(drug)
            }
        }

        connection?.close()
        return farmacos
    }
    fun fetchAllMedicos(): List<medico> {
        val medicos = mutableListOf<medico>()
        val connection = getConnection()
        val statement = connection?.createStatement()

        val resultSet = statement?.executeQuery("""
            SELECT * 
            FROM medico 
            INNER JOIN persona ON medico.id_persona = persona.id_persona
        """)

        if (resultSet != null) {
            while (resultSet.next()) {
                val med = medico(
                    resultSet.getInt("id_medico"),
                    resultSet.getInt("id_persona"),
                    resultSet.getString("especializacion"),
                    resultSet.getBoolean("habilitado")
                )
                medicos.add(med)
            }
        }

        connection?.close()
        return medicos
    }

    fun fetchAllFarmaceuticos(): List<farmaceutico> {
        val farmaceuticos = mutableListOf<farmaceutico>()
        val connection = getConnection()
        val statement = connection?.createStatement()

        val resultSet = statement?.executeQuery("""
            SELECT * 
            FROM farmaceutico 
            INNER JOIN persona ON farmaceutico.id_persona = persona.id_persona
        """)

        if (resultSet != null) {
            while (resultSet.next()) {
                val farm = farmaceutico(
                    resultSet.getInt("id_farmaceutico"),
                    resultSet.getInt("id_persona"),
                    resultSet.getBoolean("habilitado")
                )
                farmaceuticos.add(farm)
            }
        }

        connection?.close()
        return farmaceuticos
    }

    fun updateMedicoHabilitado(id_medico: Int, habilitado: Boolean): Boolean {
        val connection = getConnection()
        val statement = connection?.createStatement()

        val rowsAffected = statement?.executeUpdate("""
            UPDATE medico 
            SET habilitado = $habilitado 
            WHERE id_medico = $id_medico
        """)

        connection?.close()
        return rowsAffected == 1
    }

    fun updateFarmaceuticoHabilitado(id_farmaceutico: Int, habilitado: Boolean): Boolean {
        val connection = getConnection()
        val statement = connection?.createStatement()

        val rowsAffected = statement?.executeUpdate("""
            UPDATE farmaceutico 
            SET habilitado = $habilitado 
            WHERE id_farmaceutico = $id_farmaceutico
        """)

        connection?.close()
        return rowsAffected == 1
    }

}



