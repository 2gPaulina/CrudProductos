package com.example.crudproductos

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProductosActivity : AppCompatActivity() {

    private var tokenJwt: String? = null
    private lateinit var tokenFormateado: String

    private lateinit var rvProductos: RecyclerView
    private lateinit var adapter: ProductoAdapter

    private var listaProductos: List<Producto> = listOf()
    private var listaCategorias: List<Categoria> = listOf()
    private var listaMarcas: List<Marca> = listOf()
    private var listaProveedores: List<Proveedor> = listOf()
    private var listaDistribuidores: List<Distribuidor> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_productos)

        tokenJwt = intent.getStringExtra("TOKEN_JWT")
        if (tokenJwt.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Token no recibido", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        tokenFormateado = "Bearer $tokenJwt"

        rvProductos = findViewById(R.id.rvProductos)
        rvProductos.layoutManager = LinearLayoutManager(this)

        adapter = ProductoAdapter(
            listaProductos = listOf(),
            onEditarClick = { producto -> mostrarFormularioProducto(producto) },
            onEliminarClick = { producto -> confirmarEliminacion(producto) }
        )
        rvProductos.adapter = adapter

        findViewById<Button>(R.id.btnAlta).setOnClickListener {
            mostrarFormularioProducto(null)
        }

        findViewById<Button>(R.id.btnCerrarSesion).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        cargarCategoriasDesdeApi()
        cargarMarcasDesdeApi()
        cargarProveedoresYDistribuidores()
        cargarProductosDesdeApi()
    }

    private fun cargarProductosDesdeApi() {
        RetrofitClient.instance.obtenerProductos().enqueue(object : Callback<List<Producto>> {
            override fun onResponse(call: Call<List<Producto>>, response: Response<List<Producto>>) {
                if (response.isSuccessful && response.body() != null) {
                    listaProductos = response.body()!!
                    adapter.actualizarLista(listaProductos)
                }
            }
            override fun onFailure(call: Call<List<Producto>>, t: Throwable) {}
        })
    }

    private fun cargarCategoriasDesdeApi(onComplete: (() -> Unit)? = null) {
        RetrofitClient.instance.obtenerCategorias().enqueue(object : Callback<List<Categoria>> {
            override fun onResponse(call: Call<List<Categoria>>, response: Response<List<Categoria>>) {
                if (response.isSuccessful && response.body() != null) {
                    listaCategorias = response.body()!!
                    onComplete?.invoke()
                }
            }
            override fun onFailure(call: Call<List<Categoria>>, t: Throwable) {}
        })
    }

    private fun cargarMarcasDesdeApi(onComplete: (() -> Unit)? = null) {
        RetrofitClient.instance.obtenerMarcas().enqueue(object : Callback<List<Marca>> {
            override fun onResponse(call: Call<List<Marca>>, response: Response<List<Marca>>) {
                if (response.isSuccessful && response.body() != null) {
                    listaMarcas = response.body()!!
                    onComplete?.invoke()
                }
            }
            override fun onFailure(call: Call<List<Marca>>, t: Throwable) {}
        })
    }

    private fun cargarProveedoresYDistribuidores() {
        RetrofitClient.instance.obtenerProveedores().enqueue(object : Callback<List<Proveedor>> {
            override fun onResponse(call: Call<List<Proveedor>>, response: Response<List<Proveedor>>) {
                if (response.isSuccessful && response.body() != null) listaProveedores = response.body()!!
            }
            override fun onFailure(call: Call<List<Proveedor>>, t: Throwable) {}
        })

        RetrofitClient.instance.obtenerDistribuidores().enqueue(object : Callback<List<Distribuidor>> {
            override fun onResponse(call: Call<List<Distribuidor>>, response: Response<List<Distribuidor>>) {
                if (response.isSuccessful && response.body() != null) listaDistribuidores = response.body()!!
            }
            override fun onFailure(call: Call<List<Distribuidor>>, t: Throwable) {}
        })
    }

    // ==========================================
    // 📝 FORMULARIO DE PRODUCTO (DISEÑO EXACTO)
    // ==========================================
    private fun mostrarFormularioProducto(productoExistente: Producto?) {
        val esEdicion = productoExistente != null

        val scrollView = ScrollView(this).apply {
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
        }
        scrollView.addView(container)

        // Inputs planos tradicionales con línea inferior
        val etCodigo = crearEditTextPlano("Código de Barras", productoExistente?._id, !esEdicion)
        val etNombre = crearEditTextPlano("Nombre del Producto", productoExistente?.nombre)
        val etDesc = crearEditTextPlano("Descripción", productoExistente?.descripcion)

        // Precios en horizontal
        val rowPrecios = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            isBaselineAligned = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
        }

        val etPrecioCompra = crearEditTextPlano("P. Compra ($)", productoExistente?.precio_compra?.toString(), esDecimal = true, layoutWeight = 1f)
        val etPrecioVenta = crearEditTextPlano("P. Venta ($)", productoExistente?.precio_venta?.toString(), esDecimal = true, layoutWeight = 1f)

        rowPrecios.addView(etPrecioCompra)
        val space = View(this).apply { layoutParams = LinearLayout.LayoutParams(24, 1) }
        rowPrecios.addView(space)
        rowPrecios.addView(etPrecioVenta)

        val etStock = crearEditTextPlano("Cantidad en Stock", productoExistente?.inventario?.toString(), esEntero = true)

        container.addView(etCodigo)
        container.addView(etNombre)
        container.addView(etDesc)
        container.addView(rowPrecios)
        container.addView(etStock)

        // --- SPINNER CATEGORÍA ---
        val (rowCategoria, spinnerCat, btnCat) = crearDesplegableSpinnerConBoton()

        val updateAdapterCat = {
            val nombresCat = mutableListOf("Seleccionar Categoría *")
            nombresCat.addAll(listaCategorias.map { it.nombre })
            val adapterCat = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombresCat)
            spinnerCat.adapter = adapterCat

            if (esEdicion && productoExistente?.categoria_nombre != null) {
                val pos = nombresCat.indexOf(productoExistente.categoria_nombre)
                if (pos >= 0) spinnerCat.setSelection(pos)
            }
        }
        updateAdapterCat()
        container.addView(rowCategoria)

        btnCat.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add("➕ Nueva Categoría")
            popup.menu.add("✏️ Editar Seleccionada")
            popup.menu.add("🗑️ Eliminar Seleccionada")
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "➕ Nueva Categoría" -> mostrarDialogoCrearCategoria { cargarCategoriasDesdeApi { updateAdapterCat() } }
                    "✏️ Editar Seleccionada" -> {
                        val catNombre = spinnerCat.selectedItem?.toString() ?: ""
                        val catObj = listaCategorias.find { it.nombre == catNombre }
                        if (catObj != null) mostrarDialogoEditarCategoria(catObj) { cargarCategoriasDesdeApi { updateAdapterCat() } }
                        else Toast.makeText(this, "Selecciona una categoría válida primero", Toast.LENGTH_SHORT).show()
                    }
                    "🗑️ Eliminar Seleccionada" -> {
                        val catNombre = spinnerCat.selectedItem?.toString() ?: ""
                        if (catNombre.isNotEmpty() && catNombre != "Seleccionar Categoría *") {
                            RetrofitClient.instance.eliminarCategoria(tokenFormateado, catNombre).enqueue(object : Callback<Void> {
                                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                    if (response.isSuccessful) {
                                        cargarCategoriasDesdeApi { updateAdapterCat() }
                                    }
                                }
                                override fun onFailure(call: Call<Void>, t: Throwable) {}
                            })
                        }
                    }
                }
                true
            }
            popup.show()
        }

        // --- SPINNER MARCA ---
        val (rowMarca, spinnerMarca, btnMarca) = crearDesplegableSpinnerConBoton()

        val updateAdapterMarca = {
            val nombresMarcas = mutableListOf("Seleccionar Marca *")
            nombresMarcas.addAll(listaMarcas.map { it.nombre })
            val adapterMarca = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombresMarcas)
            spinnerMarca.adapter = adapterMarca

            if (esEdicion && productoExistente?.marca_nombre != null) {
                val pos = nombresMarcas.indexOf(productoExistente.marca_nombre)
                if (pos >= 0) spinnerMarca.setSelection(pos)
            }
        }
        updateAdapterMarca()
        container.addView(rowMarca)

        btnMarca.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add("➕ Nueva Marca")
            popup.menu.add("✏️ Editar Seleccionada")
            popup.menu.add("🗑️ Eliminar Seleccionada")
            popup.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "➕ Nueva Marca" -> mostrarDialogoCrearMarca { cargarMarcasDesdeApi { updateAdapterMarca() } }
                    "✏️ Editar Seleccionada" -> {
                        val marcaNombre = spinnerMarca.selectedItem?.toString() ?: ""
                        val marcaObj = listaMarcas.find { it.nombre == marcaNombre }
                        if (marcaObj != null) mostrarDialogoEditarMarca(marcaObj) { cargarMarcasDesdeApi { updateAdapterMarca() } }
                        else Toast.makeText(this, "Selecciona una marca válida primero", Toast.LENGTH_SHORT).show()
                    }
                    "🗑️ Eliminar Seleccionada" -> {
                        val marcaNombre = spinnerMarca.selectedItem?.toString() ?: ""
                        if (marcaNombre.isNotEmpty() && marcaNombre != "Seleccionar Marca *") {
                            RetrofitClient.instance.eliminarMarca(tokenFormateado, marcaNombre).enqueue(object : Callback<Void> {
                                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                    if (response.isSuccessful) {
                                        cargarMarcasDesdeApi { updateAdapterMarca() }
                                    }
                                }
                                override fun onFailure(call: Call<Void>, t: Throwable) {}
                            })
                        }
                    }
                }
                true
            }
            popup.show()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (esEdicion) "Modificar Producto" else "Registrar Producto")
            .setView(scrollView)
            .setPositiveButton(if (esEdicion) "Actualizar" else "Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val b = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            b.setOnClickListener {
                val codigoStr = etCodigo.text.toString().trim()
                val nombreStr = etNombre.text.toString().trim()
                val descStr = etDesc.text.toString().trim()
                val pCompraStr = etPrecioCompra.text.toString().trim()
                val pVentaStr = etPrecioVenta.text.toString().trim()
                val stockStr = etStock.text.toString().trim()

                val catStr = if (spinnerCat.selectedItemPosition > 0) spinnerCat.selectedItem.toString() else ""
                val marcaStr = if (spinnerMarca.selectedItemPosition > 0) spinnerMarca.selectedItem.toString() else ""

                // VALIDACIONES
                if (codigoStr.isEmpty()) { etCodigo.error = "Requerido"; return@setOnClickListener }
                if (codigoStr.length > 50) { etCodigo.error = "Máximo 50 caracteres"; return@setOnClickListener }

                if (nombreStr.isEmpty()) { etNombre.error = "Requerido"; return@setOnClickListener }
                if (nombreStr.length > 50) { etNombre.error = "Máximo 50 caracteres"; return@setOnClickListener }

                if (descStr.length > 50) { etDesc.error = "Máximo 50 caracteres"; return@setOnClickListener }

                val precioCompra = pCompraStr.toDoubleOrNull()
                if (precioCompra == null || precioCompra <= 0) {
                    etPrecioCompra.error = "Ingresa un precio válido"
                    return@setOnClickListener
                }

                val precioVenta = pVentaStr.toDoubleOrNull()
                if (precioVenta == null || precioVenta <= 0) {
                    etPrecioVenta.error = "Ingresa un precio válido"
                    return@setOnClickListener
                }

                if (stockStr.isEmpty()) { etStock.error = "Requerido"; return@setOnClickListener }
                if (catStr.isEmpty() || catStr == "Seleccionar Categoría *") { Toast.makeText(this, "Selecciona una Categoría", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                if (marcaStr.isEmpty() || marcaStr == "Seleccionar Marca *") { Toast.makeText(this, "Selecciona una Marca", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

                val prodData = Producto(
                    _id = codigoStr,
                    nombre = nombreStr,
                    descripcion = if (descStr.isEmpty()) "Sin descripción" else descStr,
                    precio_compra = precioCompra,
                    precio_venta = precioVenta,
                    inventario = stockStr.toIntOrNull() ?: 0,
                    categoria_nombre = catStr,
                    marca_nombre = marcaStr,
                    proveedor = com.example.crudproductos.Proveedor(nombre = "Proveedor General"),
                    fecha_creacion = "", fecha_actualizacion = "", creado_por = "", activo = true
                )

                if (esEdicion) {
                    RetrofitClient.instance.actualizarProducto(tokenFormateado, codigoStr, prodData).enqueue(object : Callback<CrudResponse> {
                        override fun onResponse(call: Call<CrudResponse>, response: Response<CrudResponse>) {
                            if (response.isSuccessful) { cargarProductosDesdeApi(); dialog.dismiss() }
                        }
                        override fun onFailure(call: Call<CrudResponse>, t: Throwable) {}
                    })
                } else {
                    RetrofitClient.instance.altaProducto(tokenFormateado, prodData).enqueue(object : Callback<CrudResponse> {
                        override fun onResponse(call: Call<CrudResponse>, response: Response<CrudResponse>) {
                            if (response.isSuccessful) { cargarProductosDesdeApi(); dialog.dismiss() }
                        }
                        override fun onFailure(call: Call<CrudResponse>, t: Throwable) {}
                    })
                }
            }
        }
        dialog.show()
    }

    // ==========================================
    // 🎨 AUXILIARES DE DISEÑO DE INTERFAZ
    // ==========================================
    private fun crearEditTextPlano(
        hint: String,
        valor: String?,
        habilitado: Boolean = true,
        esDecimal: Boolean = false,
        esEntero: Boolean = false,
        layoutWeight: Float = 0f
    ): EditText {
        val layoutParams = if (layoutWeight > 0) {
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, layoutWeight)
        } else {
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }.apply {
            setMargins(0, 8, 0, 16)
        }

        return EditText(this).apply {
            this.hint = hint
            this.isEnabled = habilitado
            this.layoutParams = layoutParams
            if (valor != null) setText(valor)
            if (esDecimal) inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            if (esEntero) inputType = InputType.TYPE_CLASS_NUMBER
        }
    }

    private fun crearDesplegableSpinnerConBoton(): Triple<LinearLayout, Spinner, ImageButton> {
        val rowLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 8, 0, 16) }
        }

        val spinner = Spinner(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val btnConfig = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_manage)
            setBackgroundColor(android.graphics.Color.parseColor("#6750A4")) // Color púrpura exacto
            setColorFilter(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(110, 110).apply {
                setMargins(16, 0, 0, 0)
            }
        }

        rowLayout.addView(spinner)
        rowLayout.addView(btnConfig)

        return Triple(rowLayout, spinner, btnConfig)
    }

    // ==========================================
    // 🏷️ MODALES PARA CATEGORÍAS
    // ==========================================
    private fun mostrarDialogoCrearCategoria(onExito: () -> Unit) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }
        val inputNombre = EditText(this).apply { hint = "Nombre Categoría (Máx 50) *" }
        val inputDesc = EditText(this).apply { hint = "Descripción (Máx 50)" }
        layout.addView(inputNombre)
        layout.addView(inputDesc)

        AlertDialog.Builder(this).setTitle("Nueva Categoría").setView(layout).setPositiveButton("Guardar") { _, _ ->
            val nom = inputNombre.text.toString().trim()
            val desc = inputDesc.text.toString().trim()

            if (nom.isEmpty()) { Toast.makeText(this, "Nombre requerido", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            if (nom.length > 50 || desc.length > 50) { Toast.makeText(this, "Máximo 50 caracteres", Toast.LENGTH_SHORT).show(); return@setPositiveButton }

            RetrofitClient.instance.crearCategoria(tokenFormateado, Categoria(nombre = nom, descripcion = desc)).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) { if (response.isSuccessful) onExito() }
                override fun onFailure(call: Call<Void>, t: Throwable) {}
            })
        }.setNegativeButton("Cancelar", null).show()
    }

    private fun mostrarDialogoEditarCategoria(cat: Categoria, onExito: () -> Unit) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }
        val inputNombre = EditText(this).apply { setText(cat.nombre) }
        val inputDesc = EditText(this).apply { setText(cat.descripcion ?: "") }
        layout.addView(inputNombre)
        layout.addView(inputDesc)

        AlertDialog.Builder(this).setTitle("Modificar Categoría").setView(layout).setPositiveButton("Actualizar") { _, _ ->
            val nom = inputNombre.text.toString().trim()
            val desc = inputDesc.text.toString().trim()

            if (nom.isEmpty()) { Toast.makeText(this, "Nombre requerido", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            if (nom.length > 50 || desc.length > 50) { Toast.makeText(this, "Máximo 50 caracteres", Toast.LENGTH_SHORT).show(); return@setPositiveButton }

            RetrofitClient.instance.actualizarCategoria(tokenFormateado, cat.nombre, Categoria(id = cat.id, nombre = nom, descripcion = desc)).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) { if (response.isSuccessful) onExito() }
                override fun onFailure(call: Call<Void>, t: Throwable) {}
            })
        }.setNegativeButton("Cancelar", null).show()
    }

    // ==========================================
    // 🏷️ MODALES PARA MARCAS (AQUÍ SÍ SE PIDEN PROVEEDOR Y DISTRIBUIDOR)
    // ==========================================
    private fun mostrarDialogoCrearMarca(onExito: () -> Unit) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val inputNombre = EditText(this).apply { hint = "Nombre Marca (Máx 50) *" }
        val inputOrigen = EditText(this).apply { hint = "Origen (Ej. México) (Máx 50)" }

        val spinnerProv = Spinner(this).apply {
            val lista = mutableListOf("Seleccionar Proveedor *")
            lista.addAll(listaProveedores.map { it.nombre })
            adapter = ArrayAdapter(this@ProductosActivity, android.R.layout.simple_spinner_dropdown_item, lista)
        }

        val spinnerDist = Spinner(this).apply {
            val lista = mutableListOf("Seleccionar Distribuidor *")
            lista.addAll(listaDistribuidores.map { it.nombre })
            adapter = ArrayAdapter(this@ProductosActivity, android.R.layout.simple_spinner_dropdown_item, lista)
        }

        layout.addView(inputNombre)
        layout.addView(inputOrigen)
        layout.addView(spinnerProv)
        layout.addView(spinnerDist)

        AlertDialog.Builder(this).setTitle("Nueva Marca").setView(layout).setPositiveButton("Guardar") { _, _ ->
            val nom = inputNombre.text.toString().trim()
            val origen = inputOrigen.text.toString().trim()
            val provSeleccionado = if (spinnerProv.selectedItemPosition > 0) spinnerProv.selectedItem.toString() else ""
            val distSeleccionado = if (spinnerDist.selectedItemPosition > 0) spinnerDist.selectedItem.toString() else ""

            if (nom.isEmpty()) { Toast.makeText(this, "Nombre requerido", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            if (nom.length > 50 || origen.length > 50) { Toast.makeText(this, "Máximo 50 caracteres", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            if (provSeleccionado.isEmpty()) { Toast.makeText(this, "Selecciona un Proveedor", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            if (distSeleccionado.isEmpty()) { Toast.makeText(this, "Selecciona un Distribuidor", Toast.LENGTH_SHORT).show(); return@setPositiveButton }

            val nuevaMarca = Marca(
                nombre = nom,
                origen = if (origen.isEmpty()) null else origen,
                proveedor = provSeleccionado,
                distribuidor = distSeleccionado
            )

            RetrofitClient.instance.crearMarca(tokenFormateado, nuevaMarca).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) { if (response.isSuccessful) onExito() }
                override fun onFailure(call: Call<Void>, t: Throwable) {}
            })
        }.setNegativeButton("Cancelar", null).show()
    }

    private fun mostrarDialogoEditarMarca(marca: Marca, onExito: () -> Unit) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val inputNombre = EditText(this).apply { setText(marca.nombre) }
        val inputOrigen = EditText(this).apply { setText(marca.origen ?: "") }

        val listProv = mutableListOf("Seleccionar Proveedor *").apply { addAll(listaProveedores.map { it.nombre }) }
        val spinnerProv = Spinner(this).apply {
            adapter = ArrayAdapter(this@ProductosActivity, android.R.layout.simple_spinner_dropdown_item, listProv)
            val pos = listProv.indexOf(marca.proveedor ?: "")
            if (pos >= 0) setSelection(pos)
        }

        val listDist = mutableListOf("Seleccionar Distribuidor *").apply { addAll(listaDistribuidores.map { it.nombre }) }
        val spinnerDist = Spinner(this).apply {
            adapter = ArrayAdapter(this@ProductosActivity, android.R.layout.simple_spinner_dropdown_item, listDist)
            val pos = listDist.indexOf(marca.distribuidor ?: "")
            if (pos >= 0) setSelection(pos)
        }

        layout.addView(inputNombre)
        layout.addView(inputOrigen)
        layout.addView(spinnerProv)
        layout.addView(spinnerDist)

        AlertDialog.Builder(this).setTitle("Modificar Marca").setView(layout).setPositiveButton("Actualizar") { _, _ ->
            val nom = inputNombre.text.toString().trim()
            val origen = inputOrigen.text.toString().trim()
            val provSeleccionado = if (spinnerProv.selectedItemPosition > 0) spinnerProv.selectedItem.toString() else ""
            val distSeleccionado = if (spinnerDist.selectedItemPosition > 0) spinnerDist.selectedItem.toString() else ""

            if (nom.isEmpty()) { Toast.makeText(this, "Nombre requerido", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            if (nom.length > 50 || origen.length > 50) { Toast.makeText(this, "Máximo 50 caracteres", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            if (provSeleccionado.isEmpty()) { Toast.makeText(this, "Selecciona un Proveedor", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            if (distSeleccionado.isEmpty()) { Toast.makeText(this, "Selecciona un Distribuidor", Toast.LENGTH_SHORT).show(); return@setPositiveButton }

            val marcaEditada = Marca(
                id = marca.id,
                nombre = nom,
                origen = if (origen.isEmpty()) null else origen,
                proveedor = provSeleccionado,
                distribuidor = distSeleccionado
            )

            RetrofitClient.instance.actualizarMarca(tokenFormateado, marca.nombre, marcaEditada).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) { if (response.isSuccessful) onExito() }
                override fun onFailure(call: Call<Void>, t: Throwable) {}
            })
        }.setNegativeButton("Cancelar", null).show()
    }

    private fun confirmarEliminacion(producto: Producto) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Borrado")
            .setMessage("¿Deseas eliminar '${producto.nombre}'?")
            .setPositiveButton("Borrar") { _, _ ->
                RetrofitClient.instance.bajaProducto(tokenFormateado, producto._id).enqueue(object : Callback<CrudResponse> {
                    override fun onResponse(call: Call<CrudResponse>, response: Response<CrudResponse>) {
                        if (response.isSuccessful) cargarProductosDesdeApi()
                    }
                    override fun onFailure(call: Call<CrudResponse>, t: Throwable) {}
                })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}