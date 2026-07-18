package com.example.crudproductos

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProductosActivity : AppCompatActivity() {

    private var tokenJwt: String? = null

    // Variables globales de los campos de texto para facilitar el borrado automático
    private lateinit var etCodigo: EditText
    private lateinit var etNombre: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etPrecioCompra: EditText
    private lateinit var etPrecioVenta: EditText
    private lateinit var etInventario: EditText
    private lateinit var etCategoria: EditText
    private lateinit var etMarca: EditText
    private lateinit var etProvNombre: EditText
    private lateinit var etProvDireccion: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_productos)

        // Recuperamos el Token JWT generado en el login del usuario encargado
        tokenJwt = intent.getStringExtra("TOKEN_JWT")
        val tokenFormateado = "Bearer $tokenJwt"

        // Enlazamos las vistas del layout XML
        etCodigo = findViewById(R.id.etCodigoBarras)
        etNombre = findViewById(R.id.etNombreProducto)
        etDescripcion = findViewById(R.id.etDescripcion)
        etPrecioCompra = findViewById(R.id.etPrecioCompra)
        etPrecioVenta = findViewById(R.id.etPrecioVenta)
        etInventario = findViewById(R.id.etInventario)
        etCategoria = findViewById(R.id.etCategoria)
        etMarca = findViewById(R.id.etMarca)
        etProvNombre = findViewById(R.id.etProveedorNombre)
        etProvDireccion = findViewById(R.id.etProveedorDireccion)

        val btnAlta = findViewById<Button>(R.id.btnAlta)
        val btnBaja = findViewById<Button>(R.id.btnBaja)
        val btnConsulta = findViewById<Button>(R.id.btnConsulta)
        val btnVenta = findViewById<Button>(R.id.btnVenta)
        val btnCerrarSesion = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCerrarSesion)

        // ==========================================
        // 🔍 ACCIÓN: CONSULTAR DISPONIBILIDAD (R1)
        // ==========================================
        btnConsulta.setOnClickListener {
            val codigo = etCodigo.text.toString().trim()
            if (codigo.isEmpty()) {
                Toast.makeText(this, "Por favor escribe un código de barras", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            RetrofitClient.instance.consultarProducto(tokenFormateado, codigo).enqueue(object : Callback<Producto> {
                override fun onResponse(call: Call<Producto>, response: Response<Producto>) {
                    if (response.isSuccessful && response.body() != null) {
                        val prod = response.body()!!
                        // Poblamos la pantalla con los datos guardados en la nube
                        etNombre.setText(prod.nombre)
                        etDescripcion.setText(prod.descripcion)
                        etPrecioCompra.setText(prod.precio_compra.toString())
                        etPrecioVenta.setText(prod.precio_venta.toString())
                        etInventario.setText(prod.inventario.toString())
                        etCategoria.setText(prod.categoria_nombre)
                        etMarca.setText(prod.marca_nombre)
                        etProvNombre.setText(prod.proveedor.nombre)
                        etProvDireccion.setText(prod.proveedor.direccion)
                        Toast.makeText(this@ProductosActivity, "Producto localizado en MongoDB", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProductosActivity, "El artículo no se encuentra registrado", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<Producto>, t: Throwable) {
                    Toast.makeText(this@ProductosActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // ==========================================
        // ➕ ACCIÓN: REGISTRAR / ALTA COMPLETA (R2)
        // ==========================================
        btnAlta.setOnClickListener {
            val idStr = etCodigo.text.toString().trim()
            val nomStr = etNombre.text.toString().trim()

            if (idStr.isEmpty() || nomStr.isEmpty()) {
                Toast.makeText(this, "Código y Nombre son campos obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Mapeamos el objeto Producto respetando la estructura esperada por Pydantic
            val nuevoProd = Producto(
                _id = idStr,
                nombre = nomStr,
                descripcion = etDescripcion.text.toString().trim().ifEmpty { "Sin descripción" },
                precio_compra = etPrecioCompra.text.toString().toDoubleOrNull() ?: 0.0,
                precio_venta = etPrecioVenta.text.toString().toDoubleOrNull() ?: 0.0,
                inventario = etInventario.text.toString().toIntOrNull() ?: 0,
                categoria_nombre = etCategoria.text.toString().trim().ifEmpty { "General" },
                marca_nombre = etMarca.text.toString().trim().ifEmpty { "Genérica" },
                proveedor = Proveedor(
                    nombre = etProvNombre.text.toString().trim().ifEmpty { "No especificado" },
                    direccion = etProvDireccion.text.toString().trim().ifEmpty { "No especificada" }
                ),
                fecha_creacion = "",      // Autogenerado por el backend
                fecha_actualizacion = "", // Autogenerado por el backend
                creado_por = "",          // Resuelto por FastAPI usando el payload del JWT
                activo = true
            )

            RetrofitClient.instance.altaProducto(tokenFormateado, nuevoProd).enqueue(object : Callback<CrudResponse> {
                override fun onResponse(call: Call<CrudResponse>, response: Response<CrudResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ProductosActivity, "¡Guardado exitosamente!", Toast.LENGTH_SHORT).show()
                        // LIMPIEZA AUTOMÁTICA EXTRA: Vaciamos la interfaz para evitar clicks dobles
                        limpiarCampos()
                    } else {
                        Toast.makeText(this@ProductosActivity, "Acceso denegado (Requiere nivel Encargado)", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<CrudResponse>, t: Throwable) {
                    Toast.makeText(this@ProductosActivity, "Error al enviar: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        val btnModificar = findViewById<Button>(R.id.btnModificar)

        // ==========================================
        // ✏️ ACCIÓN: MODIFICAR / ACTUALIZAR (R2)
        // ==========================================
        btnModificar.setOnClickListener {
            val idStr = etCodigo.text.toString().trim()
            val nomStr = etNombre.text.toString().trim()

            if (idStr.isEmpty() || nomStr.isEmpty()) {
                Toast.makeText(this, "Escribe el código y el nombre del producto a modificar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Construimos el objeto con los nuevos cambios de la pantalla
            val productoActualizado = Producto(
                _id = idStr,
                nombre = nomStr,
                descripcion = etDescripcion.text.toString().trim().ifEmpty { "Sin descripción" },
                precio_compra = etPrecioCompra.text.toString().toDoubleOrNull() ?: 0.0,
                precio_venta = etPrecioVenta.text.toString().toDoubleOrNull() ?: 0.0,
                inventario = etInventario.text.toString().toIntOrNull() ?: 0,
                categoria_nombre = etCategoria.text.toString().trim().ifEmpty { "General" },
                marca_nombre = etMarca.text.toString().trim().ifEmpty { "Genérica" },
                proveedor = Proveedor(
                    nombre = etProvNombre.text.toString().trim().ifEmpty { "No especificado" },
                    direccion = etProvDireccion.text.toString().trim().ifEmpty { "No especificada" }
                ),
                fecha_creacion = "",
                fecha_actualizacion = "",
                creado_por = "",
                activo = true
            )

            // Usamos la ruta actualizarProducto de tu RetrofitClient
            RetrofitClient.instance.actualizarProducto(tokenFormateado, idStr, productoActualizado).enqueue(object : Callback<CrudResponse> {
                override fun onResponse(call: Call<CrudResponse>, response: Response<CrudResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ProductosActivity, "¡Producto actualizado en MongoDB!", Toast.LENGTH_SHORT).show()
                        limpiarCampos() // Vaciamos campos tras el cambio exitoso
                    } else {
                        Toast.makeText(this@ProductosActivity, "Error al actualizar (Requiere Encargado)", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<CrudResponse>, t: Throwable) {
                    Toast.makeText(this@ProductosActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // ==========================================
        // ❌ ACCIÓN: DAR DE BAJA / ELIMINAR (R2)
        // ==========================================
        btnBaja.setOnClickListener {
            val codigo = etCodigo.text.toString().trim()
            if (codigo.isEmpty()) {
                Toast.makeText(this, "Escribe el código del artículo a dar de baja", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            RetrofitClient.instance.bajaProducto(tokenFormateado, codigo).enqueue(object : Callback<CrudResponse> {
                override fun onResponse(call: Call<CrudResponse>, response: Response<CrudResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ProductosActivity, "Artículo dado de baja en el sistema", Toast.LENGTH_SHORT).show()
                        limpiarCampos()
                    } else {
                        Toast.makeText(this@ProductosActivity, "Error de validación o permisos insuficientes", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<CrudResponse>, t: Throwable) {
                    Toast.makeText(this@ProductosActivity, "Error de comunicación: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // ==========================================
        // 💰 ACCIÓN: PUNTO DE VENTA / RESTAR STOCK (R3)
        // ==========================================
        btnVenta.setOnClickListener {
            val codigo = etCodigo.text.toString().trim()
            if (codigo.isEmpty()) {
                Toast.makeText(this, "Escribe o escanea un código para vender", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ventaReq = VentaRequest(codigo_barras = codigo, cantidad = 1)

            RetrofitClient.instance.realizarVenta(tokenFormateado, ventaReq).enqueue(object : Callback<VentaResponse> {
                override fun onResponse(call: Call<VentaResponse>, response: Response<VentaResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val nuevoStock = response.body()!!.inventario_nuevo
                        etInventario.setText(nuevoStock.toString()) // Actualiza dinámicamente el stock en la pantalla
                        Toast.makeText(this@ProductosActivity, "¡Venta realizada! Existencia actual: $nuevoStock", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ProductosActivity, "Error en venta (Verifica existencias)", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<VentaResponse>, t: Throwable) {
                    Toast.makeText(this@ProductosActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // ==========================================
        // 🚪 ACCIÓN: CERRAR SESIÓN SEGURA
        // ==========================================
        btnCerrarSesion.setOnClickListener {
            tokenJwt = null
            val intentarRegreso = android.content.Intent(this@ProductosActivity, LoginActivity::class.java)
            startActivity(intentarRegreso)
            finish()
            Toast.makeText(this, "Sesión finalizada correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    // ==========================================
    // ✨ FUNCIÓN AUXILIAR DE LIMPIEZA AUTOMÁTICA
    // ==========================================
    private fun limpiarCampos() {
        etCodigo.setText("")
        etNombre.setText("")
        etDescripcion.setText("")
        etPrecioCompra.setText("")
        etPrecioVenta.setText("")
        etInventario.setText("")
        etCategoria.setText("")
        etMarca.setText("")
        etProvNombre.setText("")
        etProvDireccion.setText("")
        etCodigo.requestFocus() // Coloca el foco de escritura en el código de barras automáticamente
    }
}