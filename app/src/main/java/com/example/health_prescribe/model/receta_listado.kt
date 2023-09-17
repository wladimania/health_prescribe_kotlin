package com.example.health_prescribe.model

data class receta_listado(
    val id_receta: Int?= null,
    val id_farmaco: Int,
    val cantidad: Int?= null,
    val dosis: Int,
    val id_receta_listado: Int?= null,
    val aplicacion: String
)
