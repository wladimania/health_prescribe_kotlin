package com.example.health_prescribe.model

data class FarmacoDisplay(
    val id_farmaco: Int,
    val nombre_generico: String,
    val cantidad: Int,
    val dosis: Int,
    val aplicacion: String
)

