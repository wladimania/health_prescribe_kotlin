package com.example.health_prescribe.model

data class medico(
    val id_medico: Int,
    val id_persona: Int,
    val especializacion: String,
    val habilitado: Boolean
)
