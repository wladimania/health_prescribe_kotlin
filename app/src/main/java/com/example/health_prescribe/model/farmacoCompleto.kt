package com.example.health_prescribe.model

import java.io.Serializable

data class farmacoCompleto(
    var id_farmaco: Int,
    var nombre_generico: String,
    var forma_farmaceutica: String,
    var inventario: Int,
    var concentracion: String,
    var proveedor: proveedor,
    var bar_code: String
) : Serializable
