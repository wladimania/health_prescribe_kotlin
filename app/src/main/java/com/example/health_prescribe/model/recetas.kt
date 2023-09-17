package com.example.health_prescribe.model

import java.util.Date
import java.io.Serializable
data class recetas(
    val id_receta: Int?= null,
    val id_medico: Int,
    val id_paciente: Int,
    val estado: String?, // <-- Ahora puede ser nulo
    val id_farmaceutico: Int? = null,
    val codigo_receta: String?, // <-- Ahora puede ser nulo
    val create_asistido: Boolean,
    val fecha_create: Date?, // <-- Ahora puede ser nulo
    val fecha_entrega: Date? = null,
    val nombreApellido: String?,
) : Serializable

