package com.example.crudproductos

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.Normalizer

class ProductosActivity : AppCompatActivity() {

    private var tokenJwt: String? = null
    private lateinit var tokenFormateado: String

    private lateinit var rvProductos: RecyclerView
    private lateinit var adapter: ProductoAdapter

    private var listaProductos: List<Producto> = listOf()
    private var listaCategorias: List<Categoria> = listOf()
    private var listaMarcas: List<Marca> = listOf()

    private var etCodigoFormRef: EditText? = null

    private val barcodeLauncherFormulario = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val codigoEscaneado = result.contents
            etCodigoFormRef?.setText(codigoEscaneado)
            Toast.makeText(this, "Código detectado: $codigoEscaneado", Toast.LENGTH_SHORT).show()
        }
    }

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

    // ==========================================
    // 📝 FORMULARIO CONECTADO AL XML
    // ==========================================
    private fun mostrarFormularioProducto(productoExistente: Producto?) {
        val esEdicion = productoExistente != null

        // Inflado directo de tu XML dialog_formulario_producto.xml
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_formulario_producto, null)

        val tvTitulo = dialogView.findViewById<TextView>(R.id.tvTituloFormulario)
        val etCodigo = dialogView.findViewById<EditText>(R.id.etCodigoForm)
        val btnEscanear = dialogView.findViewById<ImageButton>(R.id.btnEscanearFormulario)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreForm)
        val etDesc = dialogView.findViewById<EditText>(R.id.etDescForm)
        val etPrecioCompra = dialogView.findViewById<EditText>(R.id.etPrecioCompraForm)
        val etPrecioVenta = dialogView.findViewById<EditText>(R.id.etPrecioVentaForm)
        val etStock = dialogView.findViewById<EditText>(R.id.etStockForm)

        val spinnerCat = dialogView.findViewById<Spinner>(R.id.spinnerCategoriaForm)
        val btnCat = dialogView.findViewById<ImageButton>(R.id.btnConfigCategoria)

        val spinnerMarca = dialogView.findViewById<Spinner>(R.id.spinnerMarcaForm)
        val btnMarca = dialogView.findViewById<ImageButton>(R.id.btnConfigMarca)

        val etProveedor = dialogView.findViewById<EditText>(R.id.etProveedorForm)
        val etDistribuidor = dialogView.findViewById<EditText>(R.id.etDistribuidorForm)

        etCodigoFormRef = etCodigo

        // Filtros de longitud (Precios limitados a 4 dígitos)
        etCodigo.filters = arrayOf(InputFilter.LengthFilter(13))
        etPrecioCompra.filters = arrayOf(InputFilter.LengthFilter(4))
        etPrecioVenta.filters = arrayOf(InputFilter.LengthFilter(4))
        etStock.filters = arrayOf(InputFilter.LengthFilter(4))

        if (esEdicion) {
            tvTitulo?.text = "Modificar Producto"
            etCodigo.setText(productoExistente?._id)
            etCodigo.isEnabled = false
            btnEscanear.isEnabled = false
            btnEscanear.alpha = 0.4f

            etNombre.setText(productoExistente?.nombre)
            etDesc.setText(productoExistente?.descripcion)
            etPrecioCompra.setText(productoExistente?.precio_compra?.toString())
            etPrecioVenta.setText(productoExistente?.precio_venta?.toString())
            etStock.setText(productoExistente?.inventario?.toString())
        } else {
            tvTitulo?.text = "Registrar Producto"
        }

        btnEscanear.setOnClickListener {
            val options = ScanOptions()
                .setPrompt("Apunta al código de barras")
                .setBeepEnabled(true)
                .setOrientationLocked(false)
            barcodeLauncherFormulario.launch(options)
        }

        val updateAdapterCat = {
            val nombresCat = mutableListOf("Seleccionar Categoría *")
            nombresCat.addAll(listaCategorias.map { it.nombre })
            spinnerCat.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombresCat)

            if (esEdicion && productoExistente?.categoria_nombre != null) {
                val pos = nombresCat.indexOf(productoExistente.categoria_nombre)
                if (pos >= 0) spinnerCat.setSelection(pos)
            }
        }
        updateAdapterCat()

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
                    }
                    "🗑️ Eliminar Seleccionada" -> {
                        val catNombre = spinnerCat.selectedItem?.toString() ?: ""
                        if (catNombre.isNotEmpty() && catNombre != "Seleccionar Categoría *") {
                            RetrofitClient.instance.eliminarCategoria(tokenFormateado, catNombre).enqueue(object : Callback<Void> {
                                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                    if (response.isSuccessful) cargarCategoriasDesdeApi { updateAdapterCat() }
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

        val updateAdapterMarca = {
            val nombresMarcas = mutableListOf("Seleccionar Marca *")
            nombresMarcas.addAll(listaMarcas.map { it.nombre })
            spinnerMarca.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, nombresMarcas)

            if (esEdicion && productoExistente?.marca_nombre != null) {
                val pos = nombresMarcas.indexOf(productoExistente.marca_nombre)
                if (pos >= 0) spinnerMarca.setSelection(pos)
            }
        }
        updateAdapterMarca()

        spinnerMarca.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    val marcaSeleccionadaNombre = parent?.getItemAtPosition(position).toString()
                    val marcaObjeto = listaMarcas.find { it.nombre == marcaSeleccionadaNombre }

                    if (marcaObjeto != null) {
                        etProveedor.setText(marcaObjeto.proveedor ?: "Sin Proveedor asignado")
                        etDistribuidor.setText(marcaObjeto.distribuidor ?: "Sin Distribuidor asignado")
                    }
                } else {
                    etProveedor.setText("")
                    etDistribuidor.setText("")
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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
                    }
                    "🗑️ Eliminar Seleccionada" -> {
                        val marcaNombre = spinnerMarca.selectedItem?.toString() ?: ""
                        if (marcaNombre.isNotEmpty() && marcaNombre != "Seleccionar Marca *") {
                            RetrofitClient.instance.eliminarMarca(tokenFormateado, marcaNombre).enqueue(object : Callback<Void> {
                                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                                    if (response.isSuccessful) cargarMarcasDesdeApi { updateAdapterMarca() }
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
            .setView(dialogView)
            .setPositiveButton(if (esEdicion) "Actualizar" else "Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            // Fondo blanco en la barra de botones del AlertDialog para unirlo estéticamente con el XML
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))

            val btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val btnNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            btnPositive.setTextColor(Color.parseColor("#1976D2"))
            btnNegative.setTextColor(Color.parseColor("#757575"))

            btnPositive.setOnClickListener {
                val codigoStr = etCodigo.text.toString().trim()
                val nombreStr = etNombre.text.toString().trim()
                val descStr = etDesc.text.toString().trim()
                val pCompraStr = etPrecioCompra.text.toString().trim()
                val pVentaStr = etPrecioVenta.text.toString().trim()
                val stockStr = etStock.text.toString().trim()

                val catStr = if (spinnerCat.selectedItemPosition > 0) spinnerCat.selectedItem.toString() else ""
                val marcaStr = if (spinnerMarca.selectedItemPosition > 0) spinnerMarca.selectedItem.toString() else ""
                val provStr = etProveedor.text.toString().trim()

                if (codigoStr.isEmpty()) { etCodigo.error = "Requerido"; return@setOnClickListener }
                if (codigoStr.length != 13) { etCodigo.error = "Debe tener exactamente 13 dígitos"; return@setOnClickListener }
                if (nombreStr.isEmpty()) { etNombre.error = "Requerido"; return@setOnClickListener }

                val nombreIngresadoLimpio = normalizarTexto(nombreStr)
                val productoSimilar = listaProductos.find { prod ->
                    val nombreExistenteLimpio = normalizarTexto(prod.nombre)
                    val esMismoNombre = nombreExistenteLimpio == nombreIngresadoLimpio
                    if (esEdicion) esMismoNombre && prod._id != productoExistente?._id else esMismoNombre
                }

                if (productoSimilar != null) {
                    etNombre.error = "Ya existe un producto similar: '${productoSimilar.nombre}'"
                    Toast.makeText(this@ProductosActivity, "El nombre del producto ya está en uso o es muy similar", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val precioCompra = pCompraStr.toDoubleOrNull()
                if (precioCompra == null || precioCompra <= 0) { etPrecioCompra.error = "Precio inválido"; return@setOnClickListener }
                if (precioCompra > 9999) { etPrecioCompra.error = "No debe exceder $9,999"; return@setOnClickListener }

                val precioVenta = pVentaStr.toDoubleOrNull()
                if (precioVenta == null || precioVenta <= 0) { etPrecioVenta.error = "Precio inválido"; return@setOnClickListener }
                if (precioVenta > 9999) { etPrecioVenta.error = "No debe exceder $9,999"; return@setOnClickListener }

                if (stockStr.isEmpty()) { etStock.error = "Requerido"; return@setOnClickListener }
                val stockInt = stockStr.toIntOrNull()
                if (stockInt == null || stockInt < 0) { etStock.error = "Stock inválido"; return@setOnClickListener }
                if (stockInt > 9999) { etStock.error = "Máximo 9,999 unidades"; return@setOnClickListener }

                if (catStr.isEmpty() || catStr == "Seleccionar Categoría *") { Toast.makeText(this, "Selecciona una Categoría", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                if (marcaStr.isEmpty() || marcaStr == "Seleccionar Marca *") { Toast.makeText(this, "Selecciona una Marca", Toast.LENGTH_SHORT).show(); return@setOnClickListener }

                val proveedorObjeto = Proveedor(
                    id = "PROV-001",
                    nombre = if (provStr.isEmpty()) "Proveedor General" else provStr
                )

                val prodData = Producto(
                    _id = codigoStr,
                    nombre = nombreStr,
                    descripcion = if (descStr.isEmpty()) "Sin descripción" else descStr,
                    precio_compra = precioCompra,
                    precio_venta = precioVenta,
                    inventario = stockInt,
                    categoria_nombre = catStr,
                    marca_nombre = marcaStr,
                    proveedor = proveedorObjeto,
                    fecha_creacion = "2026-01-01T00:00:00",
                    fecha_actualizacion = "2026-01-01T00:00:00",
                    creado_por = "admin",
                    activo = true
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
    // 🏷️ MODALES PARA CATEGORÍAS Y MARCAS
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

        val d = AlertDialog.Builder(this).setTitle("Nueva Categoría").setView(layout).setPositiveButton("Guardar") { _, _ ->
            val nom = inputNombre.text.toString().trim()
            val desc = inputDesc.text.toString().trim()

            if (nom.isEmpty()) { Toast.makeText(this, "Nombre requerido", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            if (nom.length > 50 || desc.length > 50) { Toast.makeText(this, "Máximo 50 caracteres", Toast.LENGTH_SHORT).show(); return@setPositiveButton }

            RetrofitClient.instance.crearCategoria(tokenFormateado, Categoria(nombre = nom, descripcion = desc)).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) { if (response.isSuccessful) onExito() }
                override fun onFailure(call: Call<Void>, t: Throwable) {}
            })
        }.setNegativeButton("Cancelar", null).create()

        d.setOnShowListener {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#1976D2"))
            d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#757575"))
        }
        d.show()
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

        val d = AlertDialog.Builder(this).setTitle("Modificar Categoría").setView(layout).setPositiveButton("Actualizar") { _, _ ->
            val nom = inputNombre.text.toString().trim()
            val desc = inputDesc.text.toString().trim()

            if (nom.isEmpty()) { Toast.makeText(this, "Nombre requerido", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            if (nom.length > 50 || desc.length > 50) { Toast.makeText(this, "Máximo 50 caracteres", Toast.LENGTH_SHORT).show(); return@setPositiveButton }

            RetrofitClient.instance.actualizarCategoria(tokenFormateado, cat.nombre, Categoria(id = cat.id, nombre = nom, descripcion = desc)).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) { if (response.isSuccessful) onExito() }
                override fun onFailure(call: Call<Void>, t: Throwable) {}
            })
        }.setNegativeButton("Cancelar", null).create()

        d.setOnShowListener {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#1976D2"))
            d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#757575"))
        }
        d.show()
    }

    private fun mostrarDialogoCrearMarca(onExito: () -> Unit) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val inputNombre = EditText(this).apply { hint = "Nombre Marca (Máx 50) *" }
        val inputOrigen = EditText(this).apply { hint = "Origen (Ej. México) (Máx 50)" }
        val inputProveedor = EditText(this).apply { hint = "Proveedor *" }
        val inputDistribuidor = EditText(this).apply { hint = "Distribuidor *" }

        layout.addView(inputNombre)
        layout.addView(inputOrigen)
        layout.addView(inputProveedor)
        layout.addView(inputDistribuidor)

        val d = AlertDialog.Builder(this).setTitle("Nueva Marca").setView(layout).setPositiveButton("Guardar") { _, _ ->
            val nom = inputNombre.text.toString().trim()
            val origen = inputOrigen.text.toString().trim()
            val prov = inputProveedor.text.toString().trim()
            val dist = inputDistribuidor.text.toString().trim()

            if (nom.isEmpty()) { Toast.makeText(this, "Nombre requerido", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            if (nom.length > 50 || origen.length > 50) { Toast.makeText(this, "Máximo 50 caracteres", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            if (prov.isEmpty()) { Toast.makeText(this, "Escribe el Proveedor", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            if (dist.isEmpty()) { Toast.makeText(this, "Escribe el Distribuidor", Toast.LENGTH_SHORT).show(); return@setPositiveButton }

            val nuevaMarca = Marca(
                nombre = nom,
                origen = if (origen.isEmpty()) null else origen,
                proveedor = prov,
                distribuidor = dist
            )

            RetrofitClient.instance.crearMarca(tokenFormateado, nuevaMarca).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) { if (response.isSuccessful) onExito() }
                override fun onFailure(call: Call<Void>, t: Throwable) {}
            })
        }.setNegativeButton("Cancelar", null).create()

        d.setOnShowListener {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#1976D2"))
            d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#757575"))
        }
        d.show()
    }

    private fun mostrarDialogoEditarMarca(marca: Marca, onExito: () -> Unit) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        val inputNombre = EditText(this).apply { setText(marca.nombre) }
        val inputOrigen = EditText(this).apply { setText(marca.origen ?: "") }
        val inputProveedor = EditText(this).apply { setText(marca.proveedor ?: "") }
        val inputDistribuidor = EditText(this).apply { setText(marca.distribuidor ?: "") }

        layout.addView(inputNombre)
        layout.addView(inputOrigen)
        layout.addView(inputProveedor)
        layout.addView(inputDistribuidor)

        val d = AlertDialog.Builder(this).setTitle("Modificar Marca").setView(layout).setPositiveButton("Actualizar") { _, _ ->
            val nom = inputNombre.text.toString().trim()
            val origen = inputOrigen.text.toString().trim()
            val prov = inputProveedor.text.toString().trim()
            val dist = inputDistribuidor.text.toString().trim()

            if (nom.isEmpty()) { Toast.makeText(this, "Nombre requerido", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            if (nom.length > 50 || origen.length > 50) { Toast.makeText(this, "Máximo 50 caracteres", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            if (prov.isEmpty()) { Toast.makeText(this, "Escribe el Proveedor", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
            if (dist.isEmpty()) { Toast.makeText(this, "Escribe el Distribuidor", Toast.LENGTH_SHORT).show(); return@setPositiveButton }

            val marcaEditada = Marca(
                id = marca.id,
                nombre = nom,
                origen = if (origen.isEmpty()) null else origen,
                proveedor = prov,
                distribuidor = dist
            )

            RetrofitClient.instance.actualizarMarca(tokenFormateado, marca.nombre, marcaEditada).enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) { if (response.isSuccessful) onExito() }
                override fun onFailure(call: Call<Void>, t: Throwable) {}
            })
        }.setNegativeButton("Cancelar", null).create()

        d.setOnShowListener {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#1976D2"))
            d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#757575"))
        }
        d.show()
    }

    private fun confirmarEliminacion(producto: Producto) {
        val d = AlertDialog.Builder(this)
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
            .create()

        d.setOnShowListener {
            d.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#1976D2"))
            d.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#757575"))
        }
        d.show()
    }

    private fun normalizarTexto(texto: String): String {
        val normalizado = Normalizer.normalize(texto, Normalizer.Form.NFD)
        val sinAcentos = Regex("\\p{InCombiningDiacriticalMarks}+").replace(normalizado, "")
        return sinAcentos.lowercase().trim().replace(Regex("\\s+"), " ")
    }
}