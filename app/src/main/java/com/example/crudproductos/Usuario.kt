package com.example.crudproductos

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ItemCarrito(val producto: Producto, var cantidad: Int)

fun Producto.obtenerTasaIva(): Double {
    val cat = this.categoria_nombre.lowercase()
    val categoriasTasaCero = listOf("canasta basica", "medicamento", "salud", "fruta", "verdura", "leche", "pan")
    return if (categoriasTasaCero.any { cat.contains(it) }) 0.0 else 0.16
}

class Usuario : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var btnVerCuenta: Button
    private lateinit var btnCerrarSesion: Button
    private lateinit var btnEscanearCamara: ImageButton
    private lateinit var adapter: ProductoUsuarioAdapter
    private lateinit var btnConsumoPropio: Button

    private lateinit var btnCoca: Button
    private lateinit var btnChidas: Button
    private lateinit var btnTrident: Button
    private lateinit var btnMazapan: Button
    private lateinit var btnGalletas: Button

    private lateinit var btnTodos: Button

    private var listaProductos = mutableListOf<Producto>()
    private val carrito = mutableListOf<ItemCarrito>()
    private var tokenJwt: String = ""

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var ubicacionActualTexto: String = "Ubicación no obtenida"

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val codigoEscaneado = result.contents
            val productoEncontrado = listaProductos.find { it._id == codigoEscaneado }
            if (productoEncontrado != null) {
                mostrarDialogoSeleccionarCantidad(productoEncontrado)
            } else {
                Toast.makeText(this, "Producto no encontrado: $codigoEscaneado", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_usuario)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val prefs = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE)
        tokenJwt = "Bearer ${prefs.getString("TOKEN", "")}"

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        solicitarUbicacionActual()

        // Vincular vistas de la interfaz
        recyclerView = findViewById(R.id.recyclerViewProductos)
        searchView = findViewById(R.id.searchView)
        btnVerCuenta = findViewById(R.id.btnVerCuenta)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)
        btnEscanearCamara = findViewById(R.id.btnEscanearCamara)
        btnConsumoPropio = findViewById(R.id.btnConsumoPropio)

        btnTodos = findViewById(R.id.btnTodos)
        btnCoca = findViewById(R.id.btnCoca)
        btnChidas = findViewById(R.id.btnChidas)
        btnTrident = findViewById(R.id.btnTrident)
        btnMazapan = findViewById(R.id.btnMazapan)
        btnGalletas = findViewById(R.id.btnGalletas)

        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = ProductoUsuarioAdapter(listaProductos) { producto ->
            mostrarDialogoSeleccionarCantidad(producto)
        }
        recyclerView.adapter = adapter

        // Listeners Principales
        btnCerrarSesion.setOnClickListener { cerrarSesion() }
        btnEscanearCamara.setOnClickListener { iniciarEscaneo() }
        btnVerCuenta.setOnClickListener { mostrarDialogoResumenCuenta() }

        btnTodos.setOnClickListener {
            // Muestra de nuevo TODOS los productos originales
            adapter.actualizarLista(listaProductos)
            searchView.setQuery("", false)
            searchView.clearFocus()
        }

        // Listeners Accesos Rápidos
        btnCoca.setOnClickListener { filtrarPorAccesoDirecto("Coca-Cola") }
        btnChidas.setOnClickListener { filtrarPorAccesoDirecto("Las Chidas") }
        btnTrident.setOnClickListener { filtrarPorAccesoDirecto("Trident") }
        btnMazapan.setOnClickListener { filtrarPorAccesoDirecto("Mazapán") }
        btnGalletas.setOnClickListener { filtrarPorAccesoDirecto("Galletas") }

        // Listener Consumo Propio Directo
        btnConsumoPropio.setOnClickListener {
            mostrarDialogoConsumoPropioDirecto()
        }

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filtrar(newText)
                return true
            }
        })

        obtenerProductosDelServidor()
    }

    private fun filtrarPorAccesoDirecto(criterio: String) {
        val listaFiltrada = listaProductos.filter { producto ->
            (producto.marca_nombre?.contains(criterio, ignoreCase = true) == true) ||
                    (producto.categoria_nombre?.contains(criterio, ignoreCase = true) == true) ||
                    (producto.nombre?.contains(criterio, ignoreCase = true) == true)
        }

        if (listaFiltrada.isEmpty()) {
            Toast.makeText(this, "No hay variantes disponibles de $criterio", Toast.LENGTH_SHORT).show()
        }

        // Actualiza el adaptador de tu RecyclerView para dibujar las variantes en pantalla
        adapter.actualizarLista(listaFiltrada)
    }


    // -------------------------------------------------------------
    // ☕ CONSUMO PROPIO DIRECTO
    // -------------------------------------------------------------
    private fun mostrarDialogoConsumoPropioDirecto() {
        val productosDisponibles = listaProductos.filter { it.inventario > 0 }

        if (productosDisponibles.isEmpty()) {
            Toast.makeText(this, "No hay productos disponibles en inventario", Toast.LENGTH_SHORT).show()
            return
        }

        val nombresProductos = productosDisponibles.map { "${it.nombre} (Stock: ${it.inventario})" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("☕ Seleccionar para Consumo Propio")
            .setItems(nombresProductos) { _, index ->
                val productoSeleccionado = productosDisponibles[index]
                mostrarDialogoCantidadConsumoPropio(productoSeleccionado)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoCantidadConsumoPropio(producto: Producto) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_seleccionar_cantidad, null)
        val tvNombre = dialogView.findViewById<TextView>(R.id.tvNombreProductoDialog)
        val tvPrecio = dialogView.findViewById<TextView>(R.id.tvPrecioUnitarioDialog)
        val tvStock = dialogView.findViewById<TextView>(R.id.tvStockDisponibleDialog)
        val etCantidad = dialogView.findViewById<EditText>(R.id.etCantidadDialog)
        val btnRestar = dialogView.findViewById<Button>(R.id.btnRestar)
        val btnSumar = dialogView.findViewById<Button>(R.id.btnSumar)
        val btnAgregar = dialogView.findViewById<Button>(R.id.btnAgregarDialog)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelarCantidadDialog)

        tvNombre.text = "☕ Consumo: ${producto.nombre}"
        tvPrecio.text = "Precio Ref: $${String.format("%.2f", producto.precio_venta)}"
        tvStock.text = "Stock: ${producto.inventario}"
        btnAgregar.text = "Confirmar Consumo"

        btnRestar.setOnClickListener {
            val actual = etCantidad.text.toString().toIntOrNull() ?: 1
            if (actual > 1) etCantidad.setText((actual - 1).toString())
        }

        btnSumar.setOnClickListener {
            val actual = etCantidad.text.toString().toIntOrNull() ?: 1
            if (actual < producto.inventario) etCantidad.setText((actual + 1).toString())
            else Toast.makeText(this, "Stock máximo alcanzado", Toast.LENGTH_SHORT).show()
        }

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnAgregar.setOnClickListener {
            val cantidad = etCantidad.text.toString().toIntOrNull() ?: 0
            if (cantidad <= 0) {
                Toast.makeText(this, "Ingresa una cantidad válida", Toast.LENGTH_SHORT).show()
            } else if (cantidad > producto.inventario) {
                Toast.makeText(this, "Excede el inventario disponible", Toast.LENGTH_SHORT).show()
            } else {
                dialog.dismiss()
                procesarConsumoPropio(producto, cantidad)
            }
        }

        btnCancelar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun procesarConsumoPropio(producto: Producto, cantidad: Int) {
        val correoActual = FirebaseAuth.getInstance().currentUser?.email ?: ""
        val request = mapOf(
            "codigo_barras" to producto._id,
            "cantidad" to cantidad.toString(),
            "correo_usuario" to correoActual,
            "motivo" to "Consumo de Empleado"
        )

        RetrofitClient.instance.registrarConsumoPropio(tokenJwt, request)
            .enqueue(object : Callback<ConsumoResponse> {
                override fun onResponse(call: Call<ConsumoResponse>, response: Response<ConsumoResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@Usuario, "☕ Consumo propio descontado de inventario", Toast.LENGTH_SHORT).show()
                        obtenerProductosDelServidor()
                    } else {
                        Toast.makeText(this@Usuario, "Error al registrar consumo", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ConsumoResponse>, t: Throwable) {
                    Toast.makeText(this@Usuario, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // -------------------------------------------------------------
    // 📷 ESCÁNER Y GPS
    // -------------------------------------------------------------
    private fun iniciarEscaneo() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
        options.setPrompt("Apunta al código de barras del producto")
        options.setCameraId(0)
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(false)
        options.setOrientationLocked(false)
        barcodeLauncher.launch(options)
    }

    private fun solicitarUbicacionActual() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val direcciones = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (!direcciones.isNullOrEmpty()) {
                        val dir = direcciones[0]
                        val calle = dir.thoroughfare ?: ""
                        val colonia = dir.subLocality ?: dir.locality ?: ""
                        ubicacionActualTexto = "$calle, $colonia".trim(',', ' ')
                    } else {
                        ubicacionActualTexto = "Lat: ${String.format("%.4f", location.latitude)}, Lon: ${String.format("%.4f", location.longitude)}"
                    }
                } catch (e: Exception) {
                    ubicacionActualTexto = "Lat: ${String.format("%.4f", location.latitude)}, Lon: ${String.format("%.4f", location.longitude)}"
                }
            } else {
                ubicacionActualTexto = "GPS no disponible"
            }
        }
    }

    private fun mostrarTicketModal(ventaRealizada: List<ItemCarrito>) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ticket, null)
        val rvProductos = dialogView.findViewById<RecyclerView>(R.id.rvProductosTicket)
        val tvSubtotal = dialogView.findViewById<TextView>(R.id.tvSubtotalTicket)
        val tvIva16 = dialogView.findViewById<TextView>(R.id.tvIva16Ticket)
        val tvIva0 = dialogView.findViewById<TextView>(R.id.tvIva0Ticket)
        val tvTotal = dialogView.findViewById<TextView>(R.id.tvTotalTicket)
        val tvFecha = dialogView.findViewById<TextView>(R.id.tvFechaTicket)
        val tvUbicacion = dialogView.findViewById<TextView>(R.id.tvUbicacionTicket)
        val btnCerrar = dialogView.findViewById<Button>(R.id.btnCerrarTicket)

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        tvFecha.text = "Fecha: ${sdf.format(Date())}"
        tvUbicacion.text = "📍 $ubicacionActualTexto"

        var subtotalSinIva = 0.0
        var totalIva16 = 0.0
        var totalIva0 = 0.0
        var totalGeneral = 0.0

        for (item in ventaRealizada) {
            val importeTotalProducto = item.producto.precio_venta * item.cantidad
            totalGeneral += importeTotalProducto
            val tasaIva = item.producto.obtenerTasaIva()
            if (tasaIva > 0.0) {
                val base = importeTotalProducto / (1.0 + tasaIva)
                val ivaMonto = importeTotalProducto - base
                subtotalSinIva += base
                totalIva16 += ivaMonto
            } else {
                subtotalSinIva += importeTotalProducto
                totalIva0 += importeTotalProducto
            }
        }

        tvSubtotal.text = "$${String.format("%.2f", subtotalSinIva)}"
        tvIva16.text = "$${String.format("%.2f", totalIva16)}"
        tvIva0.text = "$${String.format("%.2f", totalIva0)}"
        tvTotal.text = "$${String.format("%.2f", totalGeneral)}"

        rvProductos.layoutManager = LinearLayoutManager(this)
        rvProductos.adapter = TicketAdapter(ventaRealizada)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCerrar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun cerrarSesion() {
        val correoUsuario = FirebaseAuth.getInstance().currentUser?.email ?: ""
        if (correoUsuario.isNotEmpty()) {
            Toast.makeText(this, "Procesando corte de caja...", Toast.LENGTH_SHORT).show()
            RetrofitClient.instance.realizarCorteAutomatico(correoUsuario)
                .enqueue(object : Callback<CorteResponse> {
                    override fun onResponse(call: Call<CorteResponse>, response: Response<CorteResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            val total = response.body()?.total_acumulado ?: 0.0
                            val mensaje = response.body()?.message ?: "Corte registrado"
                            Toast.makeText(this@Usuario, "$mensaje. Total de ventas: $$total", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@Usuario, "Aviso: No se pudo registrar el corte automático", Toast.LENGTH_SHORT).show()
                        }
                        finalizarSalida()
                    }

                    override fun onFailure(call: Call<CorteResponse>, t: Throwable) {
                        Log.e("CORTE_ERROR", "Error de red: ${t.message}")
                        Toast.makeText(this@Usuario, "Fallo de conexión al realizar corte", Toast.LENGTH_SHORT).show()
                        finalizarSalida()
                    }
                })
        } else {
            finalizarSalida()
        }
    }

    private fun finalizarSalida() {
        FirebaseAuth.getInstance().signOut()
        val prefs = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun mostrarDialogoSeleccionarCantidad(producto: Producto) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_seleccionar_cantidad, null)
        val tvNombre = dialogView.findViewById<TextView>(R.id.tvNombreProductoDialog)
        val tvPrecio = dialogView.findViewById<TextView>(R.id.tvPrecioUnitarioDialog)
        val tvStock = dialogView.findViewById<TextView>(R.id.tvStockDisponibleDialog)
        val etCantidad = dialogView.findViewById<EditText>(R.id.etCantidadDialog)
        val btnRestar = dialogView.findViewById<Button>(R.id.btnRestar)
        val btnSumar = dialogView.findViewById<Button>(R.id.btnSumar)
        val btnAgregar = dialogView.findViewById<Button>(R.id.btnAgregarDialog)
        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelarCantidadDialog)

        tvNombre.text = producto.nombre
        tvPrecio.text = "Precio: $${String.format("%.2f", producto.precio_venta)}"
        tvStock.text = "Stock: ${producto.inventario}"

        btnRestar.setOnClickListener {
            val actual = etCantidad.text.toString().toIntOrNull() ?: 1
            if (actual > 1) etCantidad.setText((actual - 1).toString())
        }

        btnSumar.setOnClickListener {
            val actual = etCantidad.text.toString().toIntOrNull() ?: 1
            if (actual < producto.inventario) etCantidad.setText((actual + 1).toString())
            else Toast.makeText(this, "Stock máximo alcanzado", Toast.LENGTH_SHORT).show()
        }

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnAgregar.setOnClickListener {
            val cantidad = etCantidad.text.toString().toIntOrNull() ?: 0
            if (cantidad <= 0) Toast.makeText(this, "Ingresa una cantidad válida", Toast.LENGTH_SHORT).show()
            else if (cantidad > producto.inventario) Toast.makeText(this, "Excede el inventario disponible", Toast.LENGTH_SHORT).show()
            else {
                agregarAlCarrito(producto, cantidad)
                dialog.dismiss()
            }
        }

        btnCancelar.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun agregarAlCarrito(producto: Producto, cantidad: Int) {
        val itemExistente = carrito.find { it.producto._id == producto._id }
        if (itemExistente != null) {
            if (itemExistente.cantidad + cantidad > producto.inventario) {
                Toast.makeText(this, "No puedes agregar más del stock disponible", Toast.LENGTH_SHORT).show()
                return
            }
            itemExistente.cantidad += cantidad
        } else {
            carrito.add(ItemCarrito(producto, cantidad))
        }
        actualizarBotonCuenta()
        Toast.makeText(this, "${producto.nombre} agregado al carrito", Toast.LENGTH_SHORT).show()
    }

    private fun actualizarBotonCuenta() {
        val total = carrito.sumOf { it.producto.precio_venta * it.cantidad }
        val numArticulos = carrito.sumOf { it.cantidad }
        btnVerCuenta.text = "🛒 Cuenta ($numArticulos art.) - $${String.format("%.2f", total)}"
    }

    private fun mostrarDialogoResumenCuenta() {
        if (carrito.isEmpty()) {
            Toast.makeText(this, "El carrito está vacío", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_resumen_cuenta, null)
        val rvCarrito = dialogView.findViewById<RecyclerView>(R.id.rvCarrito)
        val tvTotal = dialogView.findViewById<TextView>(R.id.tvTotalCuentaDialog)
        val btnCobrar = dialogView.findViewById<Button>(R.id.btnCobrarDialog)
        val btnVaciar = dialogView.findViewById<Button>(R.id.btnVaciarDialog)
        val btnSeguir = dialogView.findViewById<Button>(R.id.btnSeguirComprandoDialog)

        rvCarrito.layoutManager = LinearLayoutManager(this)
        rvCarrito.adapter = CarritoAdapter(carrito)

        val totalGeneral = carrito.sumOf { it.producto.precio_venta * it.cantidad }
        tvTotal.text = "$${String.format("%.2f", totalGeneral)}"

        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCobrar.setOnClickListener {
            dialog.dismiss()
            procesarVentaCarrito()
        }

        btnVaciar.setOnClickListener {
            carrito.clear()
            actualizarBotonCuenta()
            dialog.dismiss()
            Toast.makeText(this, "Carrito vaciado", Toast.LENGTH_SHORT).show()
        }

        btnSeguir.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun procesarVentaCarrito() {
        if (carrito.isEmpty()) return
        val ventaTicket = ArrayList(carrito)
        var ventasExitosas = 0
        val totalVentas = carrito.size
        val correoVendedor = FirebaseAuth.getInstance().currentUser?.email ?: ""

        for (item in carrito) {
            val requestVenta = VentaRequest(
                codigo_barras = item.producto._id,
                cantidad = item.cantidad,
                correo_vendedor = correoVendedor
            )
            RetrofitClient.instance.realizarVenta(tokenJwt, requestVenta)
                .enqueue(object : Callback<VentaResponse> {
                    override fun onResponse(call: Call<VentaResponse>, response: Response<VentaResponse>) {
                        ventasExitosas++
                        if (ventasExitosas == totalVentas) {
                            Toast.makeText(this@Usuario, "¡Venta completada con éxito!", Toast.LENGTH_SHORT).show()
                            mostrarTicketModal(ventaTicket)
                            carrito.clear()
                            actualizarBotonCuenta()
                            obtenerProductosDelServidor()
                        }
                    }

                    override fun onFailure(call: Call<VentaResponse>, t: Throwable) {
                        Toast.makeText(this@Usuario, "Error en producto: ${item.producto.nombre}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    private fun obtenerProductosDelServidor() {
        RetrofitClient.instance.obtenerTodosLosProductos(tokenJwt)
            .enqueue(object : Callback<List<Producto>> {
                override fun onResponse(call: Call<List<Producto>>, response: Response<List<Producto>>) {
                    if (response.isSuccessful && response.body() != null) {
                        listaProductos.clear()
                        val productosConStock = response.body()!!.filter { it.inventario > 0 }
                        listaProductos.addAll(productosConStock)
                        adapter.actualizarLista(listaProductos)
                    } else {
                        Toast.makeText(this@Usuario, "Error al cargar productos", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<List<Producto>>, t: Throwable) {
                    Log.e("API_ERROR", "Error de red: ${t.message}")
                    Toast.makeText(this@Usuario, "Fallo de conexión", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun filtrar(texto: String?) {
        val productosDisponibles = listaProductos.filter { it.inventario > 0 }
        val listaFiltrada = if (texto.isNullOrEmpty()) {
            productosDisponibles
        } else {
            productosDisponibles.filter { it.nombre.lowercase().contains(texto.lowercase()) }
        }
        adapter.actualizarLista(listaFiltrada)
    }

    private fun agregarPorAccesoDirecto(busqueda: String) {
        val productoEncontrado = listaProductos.find {
            it.nombre.lowercase().contains(busqueda.lowercase()) && it.inventario > 0
        }
        if (productoEncontrado != null) {
            mostrarDialogoSeleccionarCantidad(productoEncontrado)
        } else {
            Toast.makeText(this, "Producto '$busqueda' no disponible en stock", Toast.LENGTH_SHORT).show()
        }
    }
}