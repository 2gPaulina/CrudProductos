package com.example.crudproductos

import android.content.Intent
import android.os.Bundle
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProductosActivity : AppCompatActivity() {

    private var tokenJwt: String? = null
    private lateinit var tokenFormateado: String

    private lateinit var etCodigo: EditText
    private lateinit var etNombre: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var etPrecioCompra: EditText
    private lateinit var etPrecioVenta: EditText
    private lateinit var etInventario: EditText

    private lateinit var spinnerCategoria: AutoCompleteTextView
    private lateinit var spinnerMarca: AutoCompleteTextView

    private var listaCategorias: List<Categoria> = listOf()
    private var listaMarcas: List<Marca> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_productos)

        tokenJwt = intent.getStringExtra("TOKEN_JWT")
        if (tokenJwt.isNullOrEmpty()) {
            Toast.makeText(this, "Error: El Token JWT no se recibió", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        tokenFormateado = "Bearer $tokenJwt"

        try {
            etCodigo = findViewById(R.id.etCodigoBarras)
            etNombre = findViewById(R.id.etNombreProducto)
            etDescripcion = findViewById(R.id.etDescripcion)
            etPrecioCompra = findViewById(R.id.etPrecioCompra)
            etPrecioVenta = findViewById(R.id.etPrecioVenta)
            etInventario = findViewById(R.id.etInventario)

            spinnerCategoria = findViewById(R.id.spinnerCategoria)
            spinnerMarca = findViewById(R.id.spinnerMarca)

            val btnAlta = findViewById<Button>(R.id.btnAlta)
            val btnBaja = findViewById<Button>(R.id.btnBaja)
            val btnConsulta = findViewById<Button>(R.id.btnConsulta)
            val btnVenta = findViewById<Button>(R.id.btnVenta)
            val btnModificar = findViewById<Button>(R.id.btnModificar)
            val btnCerrarSesion = findViewById<Button>(R.id.btnCerrarSesion)

            val btnNuevaCategoria = findViewById<Button>(R.id.btnNuevaCategoria)
            val btnNuevaMarca = findViewById<Button>(R.id.btnNuevaMarca)

            cargarCategoriasDesdeApi()
            cargarMarcasDesdeApi()

            // ====================================================================
            // 🎴 PANEL CATEGORÍAS (CORREGIDO)
            // ====================================================================
            btnNuevaCategoria.setOnClickListener {
                val opciones = arrayOf("➕ Agregar Nueva Categoría", "✏️ Modificar Categoría Existente", "📋 Eliminar Categorías")
                AlertDialog.Builder(this)
                    .setTitle("Panel de Categorías")
                    .setItems(opciones) { _, posicion ->
                        when (posicion) {
                            0 -> { // ALTA
                                val layoutForm = LinearLayout(this).apply {
                                    orientation = LinearLayout.VERTICAL
                                    setPadding(60, 40, 60, 40)
                                }
                                val inputNombre = EditText(this).apply { hint = "Nombre (Ej: Lácteos)" }
                                val inputDescripcion = EditText(this).apply { hint = "Descripción" }
                                layoutForm.addView(inputNombre)
                                layoutForm.addView(inputDescripcion)

                                AlertDialog.Builder(this)
                                    .setTitle("Nueva Categoría")
                                    .setView(layoutForm)
                                    .setPositiveButton("Guardar") { _, _ ->
                                        val nom = inputNombre.text.toString().trim()
                                        val desc = inputDescripcion.text.toString().trim().ifEmpty { "Sin descripción" }
                                        if (nom.isNotEmpty()) {
                                            val nuevaCat = Categoria(nombre = nom, descripcion = desc)
                                            RetrofitClient.instance.crearCategoria(tokenFormateado, nuevaCat).enqueue(object : Callback<Void> {
                                                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                                    if (response.isSuccessful) {
                                                        Toast.makeText(this@ProductosActivity, "Categoría Creada", Toast.LENGTH_SHORT).show()
                                                        cargarCategoriasDesdeApi()
                                                    }
                                                }
                                                override fun onFailure(call: Call<Void>, t: Throwable) {}
                                            })
                                        }
                                    }.setNegativeButton("Cancelar", null).show()
                            }
                            1 -> { // MODIFICAR (CORREGIDO: Mapeo estricto del objeto)
                                val categoriasArray = listaCategorias.map { it.nombre }.toTypedArray()
                                if (categoriasArray.isEmpty()) {
                                    Toast.makeText(this, "No hay categorías disponibles", Toast.LENGTH_SHORT).show()
                                    return@setItems
                                }
                                AlertDialog.Builder(this)
                                    .setTitle("Selecciona la Categoría a Modificar")
                                    .setItems(categoriasArray) { _, itemPos ->
                                        val catSeleccionada = listaCategorias[itemPos]

                                        val layoutMod = LinearLayout(this).apply {
                                            orientation = LinearLayout.VERTICAL
                                            setPadding(60, 40, 60, 40)
                                        }
                                        val editNombre = EditText(this).apply { setText(catSeleccionada.nombre); hint = "Nombre" }
                                        val editDescripcion = EditText(this).apply { setText(catSeleccionada.descripcion ?: ""); hint = "Descripción" }
                                        layoutMod.addView(editNombre)
                                        layoutMod.addView(editDescripcion)

                                        AlertDialog.Builder(this@ProductosActivity)
                                            .setTitle("Modificar Categoría")
                                            .setView(layoutMod)
                                            .setPositiveButton("Actualizar") { _, _ ->
                                                val nuevoNom = editNombre.text.toString().trim()
                                                val nuevoDesc = editDescripcion.text.toString().trim()
                                                if (nuevoNom.isNotEmpty()) {
                                                    val catActualizada = Categoria(id = catSeleccionada.id, nombre = nuevoNom, descripcion = nuevoDesc)
                                                    RetrofitClient.instance.actualizarCategoria(tokenFormateado, catSeleccionada.nombre, catActualizada).enqueue(object : Callback<Void> {
                                                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                                            if (response.isSuccessful) {
                                                                Toast.makeText(this@ProductosActivity, "Categoría Actualizada", Toast.LENGTH_SHORT).show()
                                                                spinnerCategoria.setText("", false) // Limpia selección actual
                                                                cargarCategoriasDesdeApi()
                                                            }
                                                        }
                                                        override fun onFailure(call: Call<Void>, t: Throwable) {}
                                                    })
                                                }
                                            }.setNegativeButton("Cancelar", null).show()
                                    }.show()
                            }
                            2 -> { // ELIMINAR (CORREGIDO: Refresco total)
                                val categoriasArray = listaCategorias.map { it.nombre }.toTypedArray()
                                if (categoriasArray.isEmpty()) {
                                    Toast.makeText(this, "No hay categorías disponibles", Toast.LENGTH_SHORT).show()
                                    return@setItems
                                }
                                AlertDialog.Builder(this)
                                    .setTitle("Eliminar Categoría")
                                    .setItems(categoriasArray) { _, itemPos ->
                                        val catSel = categoriasArray[itemPos]
                                        RetrofitClient.instance.eliminarCategoria(tokenFormateado, catSel).enqueue(object : Callback<Void> {
                                            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                                if (response.isSuccessful) {
                                                    Toast.makeText(this@ProductosActivity, "Categoría '$catSel' eliminada", Toast.LENGTH_SHORT).show()
                                                    spinnerCategoria.setText("", false) // Evita crasheo visual si estaba seleccionada
                                                    cargarCategoriasDesdeApi() // <--- Refresca la lista y los adaptadores
                                                }
                                            }
                                            override fun onFailure(call: Call<Void>, t: Throwable) {}
                                        })
                                    }.show()
                            }
                        }
                    }.show()
            }

            // ====================================================================
            // 🎴 PANEL MARCAS (CORREGIDO)
            // ====================================================================
            btnNuevaMarca.setOnClickListener {
                val opciones = arrayOf("➕ Agregar Nueva Marca", "✏️ Modificar Marca Existente", "📋 Eliminar Marcas")
                AlertDialog.Builder(this)
                    .setTitle("Panel de Marcas")
                    .setItems(opciones) { _, posicion ->
                        when (posicion) {
                            0 -> { // ALTA
                                val layoutForm = LinearLayout(this).apply {
                                    orientation = LinearLayout.VERTICAL
                                    setPadding(60, 40, 60, 40)
                                }
                                val inputNombre = EditText(this).apply { hint = "Nombre (Ej: Colgate)" }
                                val inputOrigen = EditText(this).apply { hint = "Origen (Ej: México)" }
                                val inputDistribuidor = EditText(this).apply { hint = "Distribuidor Oficial" }
                                val inputProveedor = EditText(this).apply { hint = "Proveedor Asociado" }
                                layoutForm.addView(inputNombre)
                                layoutForm.addView(inputOrigen)
                                layoutForm.addView(inputDistribuidor)
                                layoutForm.addView(inputProveedor)

                                AlertDialog.Builder(this)
                                    .setTitle("Nueva Marca")
                                    .setView(layoutForm)
                                    .setPositiveButton("Guardar") { _, _ ->
                                        val nom = inputNombre.text.toString().trim()
                                        val orig = inputOrigen.text.toString().trim().ifEmpty { "Nacional" }
                                        val dist = inputDistribuidor.text.toString().trim().ifEmpty { "Directo" }
                                        val prov = inputProveedor.text.toString().trim().ifEmpty { "General" }

                                        if (nom.isNotEmpty()) {
                                            val nuevaMarca = Marca(nombre = nom, origen = orig, distribuidor = dist, proveedor = prov)
                                            RetrofitClient.instance.crearMarca(tokenFormateado, nuevaMarca).enqueue(object : Callback<Void> {
                                                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                                    if (response.isSuccessful) {
                                                        Toast.makeText(this@ProductosActivity, "Marca Guardada", Toast.LENGTH_SHORT).show()
                                                        cargarMarcasDesdeApi()
                                                    }
                                                }
                                                override fun onFailure(call: Call<Void>, t: Throwable) {}
                                            })
                                        }
                                    }.setNegativeButton("Cancelar", null).show()
                            }
                            1 -> { // MODIFICAR
                                val marcasArray = listaMarcas.map { it.nombre }.toTypedArray()
                                if (marcasArray.isEmpty()) {
                                    Toast.makeText(this, "No hay marcas disponibles", Toast.LENGTH_SHORT).show()
                                    return@setItems
                                }
                                AlertDialog.Builder(this)
                                    .setTitle("Selecciona la Marca a Modificar")
                                    .setItems(marcasArray) { _, itemPos ->
                                        val marcaSeleccionada = listaMarcas[itemPos]

                                        val layoutMod = LinearLayout(this).apply {
                                            orientation = LinearLayout.VERTICAL
                                            setPadding(60, 40, 60, 40)
                                        }
                                        val editNombre = EditText(this).apply { setText(marcaSeleccionada.nombre); hint = "Nombre" }
                                        val editOrigen = EditText(this).apply { setText(marcaSeleccionada.origen ?: ""); hint = "Origen" }
                                        val editDistribuidor = EditText(this).apply { setText(marcaSeleccionada.distribuidor ?: ""); hint = "Distribuidor" }
                                        val editProveedor = EditText(this).apply { setText(marcaSeleccionada.proveedor ?: ""); hint = "Proveedor" }
                                        layoutMod.addView(editNombre)
                                        layoutMod.addView(editOrigen)
                                        layoutMod.addView(editDistribuidor)
                                        layoutMod.addView(editProveedor)

                                        AlertDialog.Builder(this@ProductosActivity)
                                            .setTitle("Modificar Marca")
                                            .setView(layoutMod)
                                            .setPositiveButton("Actualizar") { _, _ ->
                                                val nuevoNom = editNombre.text.toString().trim()
                                                val nuevoOrig = editOrigen.text.toString().trim()
                                                val nuevoDist = editDistribuidor.text.toString().trim()
                                                val nuevoProv = editProveedor.text.toString().trim()

                                                if (nuevoNom.isNotEmpty()) {
                                                    val marcaActualizada = Marca(id = marcaSeleccionada.id, nombre = nuevoNom, origen = nuevoOrig, distribuidor = nuevoDist, proveedor = nuevoProv)
                                                    RetrofitClient.instance.actualizarMarca(tokenFormateado, marcaSeleccionada.nombre, marcaActualizada).enqueue(object : Callback<Void> {
                                                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                                            if (response.isSuccessful) {
                                                                Toast.makeText(this@ProductosActivity, "Marca Actualizada", Toast.LENGTH_SHORT).show()
                                                                spinnerMarca.setText("", false)
                                                                cargarMarcasDesdeApi()
                                                            }
                                                        }
                                                        override fun onFailure(call: Call<Void>, t: Throwable) {}
                                                    })
                                                }
                                            }.setNegativeButton("Cancelar", null).show()
                                    }.show()
                            }
                            2 -> { // ELIMINAR (CORREGIDO: Refresco total)
                                val marcasArray = listaMarcas.map { it.nombre }.toTypedArray()
                                if (marcasArray.isEmpty()) {
                                    Toast.makeText(this, "No hay marcas disponibles", Toast.LENGTH_SHORT).show()
                                    return@setItems
                                }
                                AlertDialog.Builder(this)
                                    .setTitle("Eliminar Marca")
                                    .setItems(marcasArray) { _, itemPos ->
                                        val marcaSel = marcasArray[itemPos]
                                        RetrofitClient.instance.eliminarMarca(tokenFormateado, marcaSel).enqueue(object : Callback<Void> {
                                            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                                if (response.isSuccessful) {
                                                    Toast.makeText(this@ProductosActivity, "Marca '$marcaSel' borrada", Toast.LENGTH_SHORT).show()
                                                    spinnerMarca.setText("", false)
                                                    cargarMarcasDesdeApi() // <--- Refresca la lista y los adaptadores
                                                }
                                            }
                                            override fun onFailure(call: Call<Void>, t: Throwable) {}
                                        })
                                    }.show()
                            }
                        }
                    }.show()
            }

            // ==========================================
            // 🔍 CONSULTAR PRODUCTO
            // ==========================================
            btnConsulta.setOnClickListener {
                val codigo = etCodigo.text.toString().trim()
                if (codigo.isEmpty()) {
                    Toast.makeText(this, "Ingresa un código", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                RetrofitClient.instance.consultarProducto(tokenFormateado, codigo).enqueue(object : Callback<Producto> {
                    override fun onResponse(call: Call<Producto>, response: Response<Producto>) {
                        if (response.isSuccessful && response.body() != null) {
                            val prod = response.body()!!
                            etNombre.setText(prod.nombre)
                            etDescripcion.setText(prod.descripcion)
                            etPrecioCompra.setText(prod.precio_compra.toString())
                            etPrecioVenta.setText(prod.precio_venta.toString())
                            etInventario.setText(prod.inventario.toString())

                            // Forzamos el texto sin lanzar el filtro interno del AutoComplete
                            spinnerCategoria.setText(prod.categoria_nombre, false)
                            spinnerMarca.setText(prod.marca_nombre, false)
                            Toast.makeText(this@ProductosActivity, "Sincronizado", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ProductosActivity, "No se encontró", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<Producto>, t: Throwable) {
                        Toast.makeText(this@ProductosActivity, "Error de red", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            // ==========================================
            // ➕ REGISTRAR PRODUCTO
            // ==========================================
            btnAlta.setOnClickListener {
                val idStr = etCodigo.text.toString().trim()
                val nomStr = etNombre.text.toString().trim()
                val catSeleccionada = spinnerCategoria.text.toString().trim()
                val marcaSeleccionada = spinnerMarca.text.toString().trim()

                if (idStr.isEmpty() || nomStr.isEmpty()) {
                    Toast.makeText(this, "Campos obligatorios vacíos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val nuevoProd = Producto(
                    _id = idStr, nombre = nomStr,
                    descripcion = etDescripcion.text.toString().trim().ifEmpty { "Sin descripción" },
                    precio_compra = etPrecioCompra.text.toString().toDoubleOrNull() ?: 0.0,
                    precio_venta = etPrecioVenta.text.toString().toDoubleOrNull() ?: 0.0,
                    inventario = etInventario.text.toString().toIntOrNull() ?: 0,
                    categoria_nombre = catSeleccionada.ifEmpty { "General" },
                    marca_nombre = marcaSeleccionada.ifEmpty { "Genérica" },
                    proveedor = Proveedor(nombre = "Asociado a Marca", direccion = "Ver en Marca"),
                    fecha_creacion = "", fecha_actualizacion = "", creado_por = "", activo = true
                )

                RetrofitClient.instance.altaProducto(tokenFormateado, nuevoProd).enqueue(object : Callback<CrudResponse> {
                    override fun onResponse(call: Call<CrudResponse>, response: Response<CrudResponse>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@ProductosActivity, "¡Alta exitosa!", Toast.LENGTH_SHORT).show()
                            limpiarCampos()
                        }
                    }
                    override fun onFailure(call: Call<CrudResponse>, t: Throwable) {}
                })
            }

            // ==========================================
            // ✏️ MODIFICAR PRODUCTO
            // ==========================================
            btnModificar.setOnClickListener {
                val idStr = etCodigo.text.toString().trim()
                val nomStr = etNombre.text.toString().trim()
                val catSeleccionada = spinnerCategoria.text.toString().trim()
                val marcaSeleccionada = spinnerMarca.text.toString().trim()

                if (idStr.isEmpty() || nomStr.isEmpty()) {
                    Toast.makeText(this, "Código y Nombre requeridos", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val productoActualizado = Producto(
                    _id = idStr, nombre = nomStr,
                    descripcion = etDescripcion.text.toString().trim().ifEmpty { "Sin descripción" },
                    precio_compra = etPrecioCompra.text.toString().toDoubleOrNull() ?: 0.0,
                    precio_venta = etPrecioVenta.text.toString().toDoubleOrNull() ?: 0.0,
                    inventario = etInventario.text.toString().toIntOrNull() ?: 0,
                    categoria_nombre = catSeleccionada.ifEmpty { "General" },
                    marca_nombre = marcaSeleccionada.ifEmpty { "Genérica" },
                    proveedor = Proveedor(nombre = "Asociado a Marca", direccion = "Ver en Marca"),
                    fecha_creacion = "", fecha_actualizacion = "", creado_por = "", activo = true
                )

                RetrofitClient.instance.actualizarProducto(tokenFormateado, idStr, productoActualizado).enqueue(object : Callback<CrudResponse> {
                    override fun onResponse(call: Call<CrudResponse>, response: Response<CrudResponse>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@ProductosActivity, "¡Modificado con éxito!", Toast.LENGTH_SHORT).show()
                            limpiarCampos()
                        }
                    }
                    override fun onFailure(call: Call<CrudResponse>, t: Throwable) {}
                })
            }

            // ==========================================
            // ❌ ELIMINAR (BAJA)
            // ==========================================
            btnBaja.setOnClickListener {
                val codigo = etCodigo.text.toString().trim()
                if (codigo.isEmpty()) return@setOnClickListener
                RetrofitClient.instance.bajaProducto(tokenFormateado, codigo).enqueue(object : Callback<CrudResponse> {
                    override fun onResponse(call: Call<CrudResponse>, response: Response<CrudResponse>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@ProductosActivity, "Eliminado", Toast.LENGTH_SHORT).show()
                            limpiarCampos()
                        }
                    }
                    override fun onFailure(call: Call<CrudResponse>, t: Throwable) {}
                })
            }

            // ==========================================
            // 💰 PUNTO DE VENTA (-1 STOCK)
            // ==========================================
            btnVenta.setOnClickListener {
                val codigo = etCodigo.text.toString().trim()
                if (codigo.isEmpty()) return@setOnClickListener
                val ventaReq = VentaRequest(codigo_barras = codigo, cantidad = 1)
                RetrofitClient.instance.realizarVenta(tokenFormateado, ventaReq).enqueue(object : Callback<VentaResponse> {
                    override fun onResponse(call: Call<VentaResponse>, response: Response<VentaResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            val nuevoStock = response.body()!!.inventario_nuevo
                            etInventario.setText(nuevoStock.toString())
                            Toast.makeText(this@ProductosActivity, "Vendido", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<VentaResponse>, t: Throwable) {}
                })
            }

            btnCerrarSesion.setOnClickListener {
                tokenJwt = null
                val intentarRegreso = Intent(this@ProductosActivity, LoginActivity::class.java)
                startActivity(intentarRegreso)
                finish()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // ====================================================================
    // METODOS DE SINCRONIZACION CON MONGODB (CORREGIDOS)
    // ====================================================================
    private fun cargarCategoriasDesdeApi() {
        RetrofitClient.instance.obtenerCategorias().enqueue(object : Callback<List<Categoria>> {
            override fun onResponse(call: Call<List<Categoria>>, response: Response<List<Categoria>>) {
                if (response.isSuccessful && response.body() != null) {
                    listaCategorias = response.body()!!
                    val nombres = listaCategorias.map { it.nombre }

                    // Asignamos el adaptador asegurando que se limpie cualquier filtro previo de Android
                    val adapter = ArrayAdapter(this@ProductosActivity, android.R.layout.simple_dropdown_item_1line, nombres)
                    spinnerCategoria.setAdapter(adapter)
                }
            }
            override fun onFailure(call: Call<List<Categoria>>, t: Throwable) {}
        })
    }

    private fun cargarMarcasDesdeApi() {
        RetrofitClient.instance.obtenerMarcas().enqueue(object : Callback<List<Marca>> {
            override fun onResponse(call: Call<List<Marca>>, response: Response<List<Marca>>) {
                if (response.isSuccessful && response.body() != null) {
                    listaMarcas = response.body()!!
                    val nombres = listaMarcas.map { it.nombre }

                    // Asignamos el adaptador asegurando que se limpie cualquier filtro previo de Android
                    val adapter = ArrayAdapter(this@ProductosActivity, android.R.layout.simple_dropdown_item_1line, nombres)
                    spinnerMarca.setAdapter(adapter)
                }
            }
            override fun onFailure(call: Call<List<Marca>>, t: Throwable) {}
        })
    }

    private fun limpiarCampos() {
        etCodigo.setText("")
        etNombre.setText("")
        etDescripcion.setText("")
        etPrecioCompra.setText("")
        etPrecioVenta.setText("")
        etInventario.setText("")
        spinnerCategoria.setText("", false)
        spinnerMarca.setText("", false)
        etCodigo.requestFocus()
    }
}