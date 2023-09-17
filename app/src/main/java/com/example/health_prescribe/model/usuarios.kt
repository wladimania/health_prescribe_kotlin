package com.example.health_prescribe.model

data class usuarios(
    val id_usuario: Int,
    val id_persona: Int,
    val usuario: String,
    val clave: String,
    val rol: Int // Ejemplo: "Medico", "Paciente", "Farmaceutico", "Admin"
)
