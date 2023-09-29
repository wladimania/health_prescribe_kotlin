package com.example.health_prescribe

import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.security.identity.IdentityCredential
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.health_prescribe.model.usuarios
import org.bouncycastle.util.Fingerprint
import java.sql.PreparedStatement
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    // Variables de vista
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private var medicoId: Int = -1
    private var farmaceuticoId: Int = 0
    var patientDetails: Triple<Int, String, String>? = null  // Añadido para almacenar detalles del paciente
    var farmaceuticoDetails: Triple<Int, String, String>? = null  // Añadido para almacenar detalles del farmacéutico

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_login)

        etUsername = findViewById(R.id.et_username)
        etPassword = findViewById(R.id.et_password)

        val btnLogin = findViewById<Button>(R.id.btn_login)
        btnLogin.setOnClickListener { loginUser() }
    }

    private fun loginUser() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        /**
         *
         */
        LoginTask().execute(username, password)
        // Dentro de un método onClick o similar
        //mostrarDialogoAutenticacionBiometrica()

    }






    inner class LoginTask : AsyncTask<String, Void, Pair<usuarios?, Triple<String, String, String>?>>() {

        override fun doInBackground(vararg params: String?): Pair<usuarios?, Triple<String, String, String>?> {
            var user: usuarios? = null
            var doctorDetails: Triple<String, String, String>? = null


            try {
                val username = params[0]
                val password = params[1]

                val connection = DatabaseConnection.getConnection()

                // Usar una consulta preparada para evitar la inyección SQL
                val sql = "SELECT * FROM usuarios WHERE usuario = ? AND clave = ?"
                val statement = connection?.prepareStatement(sql) as PreparedStatement
                statement.setString(1, username)
                statement.setString(2, password)

                val resultSet = statement.executeQuery()
                if (resultSet.next()) {
                    user = usuarios(
                        resultSet.getInt("id_usuario"),
                        resultSet.getInt("id_persona"),
                        resultSet.getString("usuario"),
                        resultSet.getString("clave"),
                        resultSet.getInt("rol")
                    )
                    // Si el usuario es un médico, obten sus detalles
                    if (user.rol == 1) {
                        val personSql = "SELECT id_persona FROM usuarios WHERE id_usuario = ?"
                        val personStatement = connection.prepareStatement(personSql)
                        personStatement.setInt(1, user.id_usuario)

                        val personResultSet = personStatement.executeQuery()
                        var idPersona = -1
                        if (personResultSet.next()) {
                            idPersona = personResultSet.getInt("id_persona")
                        }

                        val doctorSql = """SELECT p.nombre AS firstName, p.apellido AS lastName, m.especializacion AS specialty
                                   FROM persona p
                                   JOIN medico m ON p.id_persona = m.id_persona
                                   WHERE p.id_persona = ? AND m.habilitado = TRUE"""
                        val doctorStatement = connection.prepareStatement(doctorSql)

                        doctorStatement.setInt(1, idPersona)

                        val doctorResultSet = doctorStatement.executeQuery()
                        if (doctorResultSet.next()) {
                            doctorDetails = Triple(
                                doctorResultSet.getString("firstName"),
                                doctorResultSet.getString("lastName"),
                                doctorResultSet.getString("specialty")
                            )
                        }
                    }
                    // Si el usuario es un paciente, obten sus detalles
                    if (user.rol == 2) {
                        val personSql = "SELECT id_persona, nombre, apellido FROM persona WHERE id_persona = ?"
                        val personStatement = connection.prepareStatement(personSql)
                        personStatement.setInt(1, user.id_persona)

                        val personResultSet = personStatement.executeQuery()
                        if (personResultSet.next()) {
                            patientDetails = Triple(
                                personResultSet.getInt("id_persona"),
                                personResultSet.getString("nombre"),
                                personResultSet.getString("apellido")
                            )
                        }
                    }
                    // Si el usuario es un farmacéutico, obten sus detalles
                    if (user.rol == 3) {
                        val personSql = "SELECT id_persona, nombre, apellido FROM persona WHERE id_persona = ?"
                        val personStatement = connection.prepareStatement(personSql)
                        personStatement.setInt(1, user.id_persona)

                        val personResultSet = personStatement.executeQuery()
                        if (personResultSet.next()) {
                            farmaceuticoDetails = Triple(
                                personResultSet.getInt("id_persona"),
                                personResultSet.getString("nombre"),
                                personResultSet.getString("apellido")
                            )
                        }
                    }

                }

                // Obtener el ID del médico si el usuario es médico (rol = 1)
                if (user?.rol == 1) {
                    val medicoSql = "SELECT id_medico FROM medico WHERE id_persona = ?"
                    val medicoStatement = connection.prepareStatement(medicoSql)
                    medicoStatement.setInt(1, user.id_usuario)

                    val medicoResultSet = medicoStatement.executeQuery()
                    if (medicoResultSet.next()) {
                        medicoId = medicoResultSet.getInt("id_medico")
                    }
                }

                // Obtener el ID del farmacéutico si el usuario es farmacéutico (rol = 3)
                if (user?.rol == 3) {
                    val farmaceuticoSql = "SELECT f.id_farmaceutico\n" +
                            "FROM farmaceutico f\n" +
                            "JOIN persona p ON f.id_persona = p.id_persona\n" +
                            "JOIN usuarios u ON p.id_persona = u.id_persona\n" +
                            "WHERE u.id_usuario = ?"
                    val farmaceuticoStatement = connection.prepareStatement(farmaceuticoSql)
                    farmaceuticoStatement.setInt(1, user.id_usuario)

                    val farmaceuticoResultSet = farmaceuticoStatement.executeQuery()
                    if (farmaceuticoResultSet.next()) {
                        farmaceuticoId = farmaceuticoResultSet.getInt("id_farmaceutico")
                    }
                }

                connection.close()
            } catch (e: Exception) {
                Log.e("DB_ERROR", "Error al consultar el usuario", e)
            }
            return Pair(user, doctorDetails)

        }

        override fun onPostExecute(result: Pair<usuarios?, Triple<String, String, String>?>) {
            val user = result.first
            val doctorDetails = result.second

            if (user != null) {
                val intent = when (user.rol) {
                    1 -> {
                        Toast.makeText(this@LoginActivity, "Bienvenido Médico", Toast.LENGTH_LONG).show()
                        Intent(this@LoginActivity, MedicoActivity::class.java).apply {
                            doctorDetails?.let {
                                putExtra("firstName", it.first)
                                putExtra("lastName", it.second)
                                putExtra("specialty", it.third)
                            }
                            putExtra("medicoId", medicoId) // Pasar el ID del médico
                            putExtra("farmaceuticoId", farmaceuticoId) // Pasar el ID del farmacéutico
                        }
                    }
                    2 -> {
                        Toast.makeText(this@LoginActivity, "Bienvenido Paciente", Toast.LENGTH_LONG).show()
                        Intent(this@LoginActivity, PacienteActivity::class.java).apply {
                            patientDetails?.let {
                                putExtra("id_persona", it.first)
                                putExtra("nombre", it.second)
                                putExtra("apellido", it.third)
                            }
                        }
                    }

                    3 -> {
                        Toast.makeText(this@LoginActivity, "Bienvenido Farmacéutico", Toast.LENGTH_LONG).show()
                        Intent(this@LoginActivity, FarmaceuticoActivity::class.java).apply {
                            farmaceuticoDetails?.let {
                                putExtra("id_persona", it.first)
                                putExtra("nombre", it.second)
                                putExtra("apellido", it.third)
                            }
                            putExtra("farmaceuticoId", farmaceuticoId)  // Pasar el ID del farmacéutico
                        }
                    }

                    4 -> {
                        Toast.makeText(this@LoginActivity, "Bienvenido Admin", Toast.LENGTH_LONG).show()
                        Intent(this@LoginActivity, AdminActivity::class.java)
                    }
                    else -> null
                }
                intent?.let { startActivity(it) }
            } else {
                Toast.makeText(this@LoginActivity, "Usuario o contraseña incorrectos", Toast.LENGTH_LONG).show()
            }
        }
    }
}
