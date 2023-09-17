package com.example.health_prescribe.model

data class persona (
    val id_persona: Int,
    val nombre: String,
    val apellido: String,
    val cedula: String,
    val rol: Int,
    val habilitado: Boolean
)