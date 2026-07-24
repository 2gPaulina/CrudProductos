package com.example.crudproductos

import com.google.gson.annotations.SerializedName

data class Categoria(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("descripcion") val descripcion: String? = null
)