package com.example.health_prescribe.model

import java.io.Serializable

data class PacienteDetalle(
    val id_cliente: Int,
    val id_persona: Int,
    val nombre: String,
    val apellido: String,
    val cedula: String,
    val telefono: String,
    val habilitado: Boolean,
    val correo: String,
    val huellaDactilar: Serializable
)
