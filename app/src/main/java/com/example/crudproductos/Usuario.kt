package com.example.crudproductos

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// Modelo auxiliar para el Carrito
data class ItemCarrito(val producto: Producto, var cantidad: Int)

class Usuario : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var btnVerCuenta: Button
    private lateinit var adapter: ProductoAdapter

    private var listaProductos = mutableListOf<Producto>()
    private val carrito = mutableListOf<ItemCarrito>() // Carrito en memoria

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

        // Recuperar Token
        val prefs = getSharedPreferences("MisPreferencias", Context.MODE_PRIVATE)
        tokenJwt = "Bearer ${prefs.getString("TOKEN", "")}"

        // Vincular Vistas
        recyclerView = findViewById(R.id.recyclerViewProductos)
        searchView = findViewById(R.id.searchView)
        btnVerCuenta = findViewById(R.id.btnVerCuenta)

        recyclerView.layoutManager = LinearLayoutManager(this)

        // Al presionar "Agregar" en un producto
        adapter = ProductoAdapter(listaProductos) { producto ->
            mostrarDialogoSeleccionarCantidad(producto)
        }
        recyclerView.adapter = adapter

        // Al presionar el botón "Ver Cuenta"
        btnVerCuenta.setOnClickListener {
            mostrarDialogoResumenCuenta()
        }

        // Descargar productos
        obtenerProductosDelServidor()

        // Buscador
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filtrar(newText)
                return true
            }
        })
    }

    // -------------------------------------------------------------
    // 🪟 VENTANA FLOTANTE 1: SELECCIONAR CANTIDAD
    // -------------------------------------------------------------
    private fun mostrarDialogoSeleccionarCantidad(producto: Producto) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "Cantidad (Máx: ${producto.inventario})"
            setText("1")
        }

        AlertDialog.Builder(this)
            .setTitle("Agregar: ${producto.nombre}")
            .setMessage("Precio unitario: $${String.format("%.2f", producto.precio_venta)}\nIngresa la cantidad a vender:")
            .setView(input)
            .setPositiveButton("Agregar") { _, _ ->
                val cantidadTexto = input.text.toString()
                val cantidad = cantidadTexto.toIntOrNull() ?: 0

                if (cantidad <= 0) {
                    Toast.makeText(this, "Ingresa una cantidad válida", Toast.LENGTH_SHORT).show()
                } else if (cantidad > producto.inventario) {
                    Toast.makeText(this, "Excede el inventario disponible", Toast.LENGTH_SHORT).show()
                } else {
                    agregarAlCarrito(producto, cantidad)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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
    // 🪟 VENTANA FLOTANTE 2: RESUMEN Y CONFIRMACIÓN DE VENTA
    // -------------------------------------------------------------
    private fun mostrarDialogoResumenCuenta() {
        if (carrito.isEmpty()) {
            Toast.makeText(this, "El carrito está vacío", Toast.LENGTH_SHORT).show()
            return
        }

        val builderDetalle = StringBuilder()
        var totalGeneral = 0.0

        for (item in carrito) {
            val subtotal = item.producto.precio_venta * item.cantidad
            totalGeneral += subtotal
            builderDetalle.append("• ${item.producto.nombre}\n")
            builderDetalle.append("  ${item.cantidad} x $${String.format("%.2f", item.producto.precio_venta)} = $${String.format("%.2f", subtotal)}\n\n")
        }

        builderDetalle.append("-----------------------------------\n")
        builderDetalle.append("TOTAL A COBRAR: $${String.format("%.2f", totalGeneral)}")

        AlertDialog.Builder(this)
            .setTitle("📋 Resumen de la Venta")
            .setMessage(builderDetalle.toString())
            .setPositiveButton("💳 Cobrar / Confirmar") { _, _ ->
                procesarVentaCarrito()
            }
            .setNeutralButton("Vaciar Carrito") { _, _ ->
                carrito.clear()
                actualizarBotonCuenta()
                Toast.makeText(this, "Carrito vaciado", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Seguir comprando", null)
            .show()
    }

    // -------------------------------------------------------------
    // 🌐 PROCESAR VENTAS CON FASTAPI
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
                            obtenerProductosDelServidor() // Recargar lista e inventario actualizado
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