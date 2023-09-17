package com.example.health_prescribe.model

data class cliente(
    val id_cliente: Int,
    val id_persona: String,
    val telefono: String,
    val habilitado: Boolean,
    val correo: String,
    val huellaDactilar: ByteArray
)
