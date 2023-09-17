    package com.example.health_prescribe.model

    import java.io.Serializable

    data class farmacoCompleto(
        val id_farmaco: Int,
        val nombre_generico: String,
        val forma_farmaceutica: String,
        val inventario: Int,
        val concentracion: String,
        val proveedor: proveedor,
        val bar_code: String
    ) : Serializable
