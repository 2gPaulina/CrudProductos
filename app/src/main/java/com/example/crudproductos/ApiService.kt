package com.example.crudproductos

import retrofit2.Call
import retrofit2.http.*

// =================================================================
// 📦 MODELOS DE DATOS COMPLETOS (ESPEJO FIEL DE TU EXAMEN)
// =================================================================

data class LoginRequest(val firebase_token: String, val correo: String)

// Estructura completa de Usuario conforme a tu tabla
data class UsuarioResponse(
    val firebase_uid: String,
    val nombre: String,
    val correo: String,
    val rol: String,
    val fecha_registro: String,
    val activo: Boolean
)

data class LoginResponse(val status: String, val token_jwt: String, val usuario: UsuarioResponse)

// Sub-objeto embebido para el Proveedor del producto

// Estructura completa de Producto conforme a tus tablas
data class Producto(
    val _id: String,
    val nombre: String,
    val descripcion: String,
    val precio_compra: Double,
    val precio_venta: Double,
    val inventario: Int,
    val categoria_nombre: String,
    val marca_nombre: String,
    val proveedor: Proveedor,
    val fecha_creacion: String,
    val fecha_actualizacion: String,
    val creado_por: String,
    val activo: Boolean,
    val tasa_iva: Double = 0.16
)
data class CrudResponse(val status: String, val message: String)
data class VentaRequest(val codigo_barras: String, val cantidad: Int = 1)
data class VentaResponse(val status: String, val inventario_nuevo: Int)

// =================================================================
// 🌐 INTERFAZ DE CONEXIÓN CON FASTAPI
// =================================================================
interface ApiService {
    @POST("auth/login")
    fun loginUsuario(@Body request: LoginRequest): Call<LoginResponse>

    @GET("productos/{codigo}")
    fun consultarProducto(
        @Header("Authorization") token: String,
        @Path("codigo") codigo: String
    ): Call<Producto>

    @POST("productos")
    fun altaProducto(
        @Header("Authorization") token: String,
        @Body producto: Producto
    ): Call<CrudResponse>

    @GET("productos")
    fun obtenerTodosLosProductos(
        @Header("Authorization") token: String
    ): Call<List<Producto>>

    @DELETE("productos/{codigo}")
    fun bajaProducto(
        @Header("Authorization") token: String,
        @Path("codigo") codigo: String
    ): Call<CrudResponse>

    @POST("ventas")
    fun realizarVenta(
        @Header("Authorization") token: String,
        @Body venta: VentaRequest
    ): Call<VentaResponse>

    @PUT("productos/{producto_id}")
    fun actualizarProducto(
        @Header("Authorization") token: String,
        @Path("producto_id") id: String,
        @Body producto: Producto
    ): Call<CrudResponse>

    @GET("productos")
    fun obtenerProductos(): Call<List<Producto>>
    @GET("categorias") // O la ruta exacta de tu FastAPI
    fun obtenerCategorias(): Call<List<Categoria>>

    @POST("categorias")
    fun crearCategoria(
        @Header("Authorization") token: String,
        @Body categoria: Categoria
    ): Call<Void> // 🌟 Cambiado a Void para solucionar el error de tipo

    @DELETE("categorias/{nombre}")
    fun eliminarCategoria(
        @Header("Authorization") token: String,
        @Path("nombre") nombre: String
    ): Call<Void> // 🌟 Agregado el método de eliminación

    // --- MARCAS ---
    @GET("marcas") // O la ruta exacta de tu FastAPI
    fun obtenerMarcas(): Call<List<Marca>>

    @POST("marcas")
    fun crearMarca(
        @Header("Authorization") token: String,
        @Body marca: Marca
    ): Call<Void> // 🌟 Cambiado a Void para solucionar el error de tipo

    @DELETE("marcas/{nombre}")
    fun eliminarMarca(
        @Header("Authorization") token: String,
        @Path("nombre") nombre: String
    ): Call<Void>

    @PUT("categorias/{nombre}")
    fun actualizarCategoria(
        @Header("Authorization") token: String,
        @Path("nombre") nombre: String,
        @Body categoria: Categoria
    ): Call<Void>

    @PUT("marcas/{nombre}")
    fun actualizarMarca(
        @Header("Authorization") token: String,
        @Path("nombre") nombre: String,
        @Body marca: Marca
    ): Call<Void>

    @GET("proveedores")
    fun obtenerProveedores(): Call<List<Proveedor>>

    @GET("distribuidores")
    fun obtenerDistribuidores(): Call<List<Distribuidor>>
}