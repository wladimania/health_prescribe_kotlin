package com.example.health_prescribe.adapter

import com.example.health_prescribe.model.farmacoCompleto
import com.example.health_prescribe.DatabaseConnection
import com.example.health_prescribe.model.FarmacoDisplay

class DatabaseHelper {
    fun fetchFarmacosFromDatabase(): List<FarmacoDisplay> {
        val farmacosList = mutableListOf<FarmacoDisplay>()

        // Utiliza tu clase DatabaseConnection para obtener una conexión a la base de datos
        val connection = DatabaseConnection.getConnection()
        val statement = connection?.createStatement()

        // Define la consulta SQL para obtener los datos
        val query = "SELECT * FROM farmacos_table"  // Ajusta esto según tus necesidades

        // Ejecuta la consulta y obtiene un objeto ResultSet
        val resultSet = statement?.executeQuery(query)

        // Itera a través del objeto ResultSet para obtener los datos
        while (resultSet?.next() == true) {
            val id = resultSet.getInt("id_farmaco")
            val nombreGenerico = resultSet.getString("nombre_generico")
            val formaFarmaceutica = resultSet.getString("forma_farmaceutica")
            val inventario = resultSet.getInt("inventario")
            val concentracion = resultSet.getString("concentracion")
            val cantidad = 0  // Este es un valor ficticio. Tendrás que encontrar la forma de obtener la cantidad real.
            val aplicacion = ""  // Este es un valor ficticio. Tendrás que encontrar la forma de obtener la aplicación real.

            val farmaco = FarmacoDisplay(id, nombreGenerico, cantidad, inventario, aplicacion)
            farmacosList.add(farmaco)
        }

        // Cierra la conexión a la base de datos
        connection?.close()

        return farmacosList
    }

    fun getProviderId(providerName: String): Int? {
        val connection = DatabaseConnection.getConnection()
        val statement = connection?.createStatement()

        val query = "SELECT id_proveedor FROM proveedor WHERE nombre_proveedor = '$providerName'"
        val resultSet = statement?.executeQuery(query)

        var providerId: Int? = null
        if (resultSet?.next() == true) {
            providerId = resultSet.getInt("id_proveedor")
        }

        connection?.close()
        return providerId
    }

    fun updateDrug(drug: farmacoCompleto): Boolean {
        val providerId = getProviderId(drug.proveedor.nombre_proveedor)
        if (providerId == null) {
            return false
        }

        val connection = DatabaseConnection.getConnection()
        val statement = connection?.createStatement()

        val updateQuery = """
            UPDATE farmaco SET
            nombre_generico = '${drug.nombre_generico}',
            inventario = ${drug.inventario},
            concentracion = '${drug.concentracion}',
            id_proveedor = $providerId
            WHERE id_farmaco = ${drug.id_farmaco}
        """.trimIndent()

        val result = statement?.executeUpdate(updateQuery)
        connection?.close()

        return result == 1
    }

}
