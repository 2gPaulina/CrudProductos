package com.example.crudproductos

import com.google.gson.annotations.SerializedName

data class Marca(
    @SerializedName("_id") val id: String? = null,
    @SerializedName("nombre") val nombre: String,
    @SerializedName("origen") val origen: String? = null,
    @SerializedName("distribuidor") val distribuidor: String? = null,
    @SerializedName("proveedor") val proveedor: String? = null
)