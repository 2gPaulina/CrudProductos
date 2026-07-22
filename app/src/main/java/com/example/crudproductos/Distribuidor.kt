package com.example.crudproductos

import com.google.gson.annotations.SerializedName

data class Distribuidor(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("nombre") val nombre: String
)