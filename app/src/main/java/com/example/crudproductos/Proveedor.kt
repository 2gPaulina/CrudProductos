package com.example.crudproductos

import com.google.gson.annotations.SerializedName

data class Proveedor(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("nombre") val nombre: String
)