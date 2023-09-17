package com.example.health_prescribe.model

data class farmaco(
    val id_farmaco: Int,
    val nombre_generico: String,
    val forma_farmaceutica: String,
    val inventario: Int,
    val concentracion: String,
    val id_proveedor: Int,
    val bar_code: String
)
