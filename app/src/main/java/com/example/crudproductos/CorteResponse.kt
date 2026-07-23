package com.example.crudproductos

import com.google.gson.annotations.SerializedName

data class CorteResponse(
    val status: String,
    val message: String,
    val total_acumulado: Double
)