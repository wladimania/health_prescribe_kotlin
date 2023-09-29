package com.example.health_prescribe

import android.os.AsyncTask
import android.util.Log
import com.example.health_prescribe.model.PacienteDetalle
import com.example.health_prescribe.model.farmaco
import com.example.health_prescribe.model.farmacoCompleto
import com.example.health_prescribe.model.proveedor
import com.example.health_prescribe.model.receta
import com.example.health_prescribe.model.receta_listado
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement


object DatabaseConnection {
    val logger: Logger = LoggerFactory.getLogger(DatabaseConnection::class.java) // Reemplaza YourClassName con el nombre de tu clase
    private const val url = "jdbc:postgresql://ec2-34-202-53-101.compute-1.amazonaws.com:5432/d39ljtg5nn3bvr?sslmode=require"
    private const val user = "dnspdplhkzkedv"
    private const val password = "4e6a1ef6e41639e819361f97326d377bf9c7ca30e585325e15585adecdc5bc3f"
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
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
            return null
        }
    }

    fun getPatientByIdhUELLA(idCliente: Int): PacienteDetalle? {
        val connection = getConnection()
        var statement: Statement? = null
        var paciente: PacienteDetalle? = null
        try {
            statement = connection!!.createStatement()
            val query =
                "SELECT * FROM cliente INNER JOIN persona ON cliente.id_persona = persona.id_persona WHERE cliente.id_cliente = $idCliente"
            val resultSet = statement.executeQuery(query)
            if (resultSet.next()) {
                val huellaDactilar = resultSet.getBytes("huellaDactilar")
                // Asegúrate de tener un constructor en PacienteDetalle que acepte estos parámetros
                paciente = PacienteDetalle(
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
            }
        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            try {
                statement?.close()
                connection?.close()
            } catch (ex: SQLException) {
                ex.printStackTrace()
            }
        }
        return paciente
    }

    // Llama a esta función en tu actividad para obtener el ID de la receta en un hilo secundario.
    fun obtenerIdClientePorCodigoRecetaAsync(codigoReceta: String, callback: (Int) -> Unit) {
        AsyncTask.execute {
            val clienteId = obtenerIdClientePorCodigoReceta(codigoReceta)
            callback(clienteId)
        }
    }
    private fun obtenerIdClientePorCodigoReceta(codigoReceta: String): Int {
        val connection = getConnection()
        val statement = connection?.createStatement()

        val query = "SELECT id_paciente FROM receta WHERE codigo_receta = '$codigoReceta'"
        val resultSet = statement?.executeQuery(query)

        var clienteId = -1

        if (resultSet?.next() == true) {
            clienteId = resultSet.getInt("id_paciente")
        }

        connection?.close()
        return clienteId
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
    @JvmStatic
    fun getFingerprintByPatientId(idPaciente: Int): ByteArray? {
        var connection: Connection? = null
        var preparedStatement: PreparedStatement? = null
        return try {
            connection = getConnection()
            val query ="SELECT \"huellaDactilar\" FROM cliente WHERE id_cliente = ?"
            preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.setInt(1, idPaciente)
            logger.info("Ejecutando consulta SQL: $query")
            val resultSet = preparedStatement?.executeQuery()
            logger.info("Result SQL: $resultSet")
            return if (resultSet?.next() == true) {
                resultSet.getBytes("huellaDactilar")
            } else {
                null
            }
        } catch (e: SQLException) {
            Log.e("Error al traer la huella", "Error al guardar la huella dactilar en la base de datos", e)
            return null
        } finally {
            preparedStatement?.close()
            connection?.close()
        }
    }
    @JvmStatic
    fun saveFingerprintToDatabase(patientId: Int, fingerprint: ByteArray): Boolean {
        var connection: Connection? = null
        var preparedStatement: PreparedStatement? = null

        return try {
            connection = getConnection()
            val query = "UPDATE cliente SET \"huellaDactilar\" = ? WHERE id_cliente = ?"
            preparedStatement = connection?.prepareStatement(query)
            preparedStatement?.setBytes(1, fingerprint)
            preparedStatement?.setInt(2, patientId)
            logger.info("Ejecutando consulta SQL: $query")
            logger.info("Datos: huellaDactilar = ${fingerprint.toString()}, id_cliente = $patientId")
            val rowsUpdated = preparedStatement?.executeUpdate() ?: 0
            rowsUpdated == 1  // Retorna true si se actualizó una fila, false en caso contrario
        } catch (e: SQLException) {
            Log.e("DBConnectionError", "Error al guardar la huella dactilar en la base de datos", e)
            false  // Retorna false si ocurrió un error
        } finally {
            // Asegurarte de cerrar los recursos en el bloque finally
            preparedStatement?.close()
            connection?.close()
        }
    }





}



