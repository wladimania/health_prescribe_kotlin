package com.example.health_prescribe.adapter

import com.example.health_prescribe.model.farmacoCompleto
import com.example.health_prescribe.DatabaseConnection

class DatabaseHelper {

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
