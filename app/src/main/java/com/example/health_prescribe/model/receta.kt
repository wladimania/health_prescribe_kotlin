package com.example.health_prescribe.model

import java.util.Date

data class receta(
    val id_receta: Int?= null,
    val id_medico: Int,
    val id_paciente: Int,
    val estado: String,
    val id_farmaceutico: Int? = null,
    val codigo_receta: String,
    val create_asistido: Boolean,
    val fecha_create: String,
    val fecha_entrega: Date? = null ,
)
