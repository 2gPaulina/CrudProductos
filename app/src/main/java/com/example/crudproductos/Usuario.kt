package com.example.crudproductos

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Modelo auxiliar para gestionar los artículos agregados al Carrito
data class ItemCarrito(val producto: Producto, var cantidad: Int)

class Usuario : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var btnVerCuenta: Button
    private lateinit var btnCerrarSesion: Button
    private lateinit var adapter: ProductoAdapter

    private var listaProductos = mutableListOf<Producto>()
    private val carrito = mutableListOf<ItemCarrito>()

    private var tokenJwt: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_usuario)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Recuperar Token JWT desde SharedPreferences (Guardado en LoginActivity)
        val prefs = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE)
        tokenJwt = "Bearer ${prefs.getString("TOKEN", "")}"

        // Vincular vistas del XML
        recyclerView = findViewById(R.id.recyclerViewProductos)
        searchView = findViewById(R.id.searchView)
        btnVerCuenta = findViewById(R.id.btnVerCuenta)
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Botón Cerrar Sesión
        btnCerrarSesion.setOnClickListener {
            cerrarSesion()
        }

        // Configuración del Adaptador de Productos (Al presionar "Agregar")
        adapter = ProductoAdapter(listaProductos) { producto ->
            mostrarDialogoSeleccionarCantidad(producto)
        }
        recyclerView.adapter = adapter

        // Botón "Ver Cuenta" (Abre el resumen flotante del Carrito)
        btnVerCuenta.setOnClickListener {
            mostrarDialogoResumenCuenta()
        }

        // Cargar productos desde FastAPI / MongoDB
        obtenerProductosDelServidor()

        // Listener del Buscador local
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filtrar(newText)
                return true
            }
        })
    }

    // -------------------------------------------------------------
    // 🚪 CERRAR SESIÓN
    // -------------------------------------------------------------
    private fun cerrarSesion() {
        FirebaseAuth.getInstance().signOut()

        val prefs = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    // -------------------------------------------------------------
    // 🪟 VENTANA FLOTANTE 1: SELECCIONAR CANTIDAD (+ / -)
    // -------------------------------------------------------------
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

        // Botón RESTAR (-)
        btnRestar.setOnClickListener {
            val actual = etCantidad.text.toString().toIntOrNull() ?: 1
            if (actual > 1) {
                etCantidad.setText((actual - 1).toString())
            }
        }

        // Botón SUMAR (+)
        btnSumar.setOnClickListener {
            val actual = etCantidad.text.toString().toIntOrNull() ?: 1
            if (actual < producto.inventario) {
                etCantidad.setText((actual + 1).toString())
            } else {
                Toast.makeText(this, "Stock máximo alcanzado", Toast.LENGTH_SHORT).show()
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnAgregar.setOnClickListener {
            val cantidadTexto = etCantidad.text.toString()
            val cantidad = cantidadTexto.toIntOrNull() ?: 0

            if (cantidad <= 0) {
                Toast.makeText(this, "Ingresa una cantidad válida", Toast.LENGTH_SHORT).show()
            } else if (cantidad > producto.inventario) {
                Toast.makeText(this, "Excede el inventario disponible", Toast.LENGTH_SHORT).show()
            } else {
                agregarAlCarrito(producto, cantidad)
                dialog.dismiss()
            }
        }

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

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
        btnVerCuenta.text = "🛒 Ver Cuenta ($numArticulos art.) - $${String.format("%.2f", total)}"
    }

    // -------------------------------------------------------------
    // 🪟 VENTANA FLOTANTE 2: RESUMEN Y CONFIRMACIÓN DE LA CUENTA
    // -------------------------------------------------------------
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

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

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

        btnSeguir.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // -------------------------------------------------------------
    // 🌐 ENVIAR VENTA Y SINCRONIZAR CON FASTAPI
    // -------------------------------------------------------------
    private fun procesarVentaCarrito() {
        var ventasExitosas = 0
        val totalVentas = carrito.size

        for (item in carrito) {
            val requestVenta = VentaRequest(codigo_barras = item.producto._id, cantidad = item.cantidad)

            RetrofitClient.instance.realizarVenta(tokenJwt, requestVenta)
                .enqueue(object : Callback<VentaResponse> {
                    override fun onResponse(call: Call<VentaResponse>, response: Response<VentaResponse>) {
                        ventasExitosas++
                        if (ventasExitosas == totalVentas) {
                            Toast.makeText(this@Usuario, "¡Venta completada con éxito!", Toast.LENGTH_LONG).show()
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
                        listaProductos.addAll(response.body()!!)
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
        val listaFiltrada = if (texto.isNullOrEmpty()) {
            listaProductos
        } else {
            listaProductos.filter { it.nombre.lowercase().contains(texto.lowercase()) }
        }
        adapter.actualizarLista(listaFiltrada)
    }
}