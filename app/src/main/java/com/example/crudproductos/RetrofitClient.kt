package com.example.crudproductos

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // REEMPLAZA "192.168.1.XX" POR LA DIRECCIÓN IP DE TI COMPUTADORA
    // Si usas el emulador de Android Studio por defecto, puedes usar "10.0.2.2"
    private const val BASE_URL = "http://10.112.19.184:8000/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}


