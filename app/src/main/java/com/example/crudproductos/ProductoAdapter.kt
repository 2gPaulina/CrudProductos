package com.example.crudproductos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductoAdapter(
    private var productos: List<Producto>,
    private val alVenderClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreProducto)
        val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        val tvExistencias: TextView = itemView.findViewById(R.id.tvExistencias)
        val btnVender: Button = itemView.findViewById(R.id.btnVender)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto_usuario, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = productos[position]
        holder.tvNombre.text = producto.nombre

        // Usamos tus variables de modelo: precio_venta e inventario
        holder.tvPrecio.text = "$ ${String.format("%.2f", producto.precio_venta)}"
        holder.tvExistencias.text = "Stock: ${producto.inventario}"

        // Si no hay existencias, deshabilitar el botón
        holder.btnVender.isEnabled = producto.inventario > 0

        holder.btnVender.setOnClickListener {
            alVenderClick(producto)
        }
    }

    override fun getItemCount(): Int = productos.size

    fun actualizarLista(nuevaLista: List<Producto>) {
        productos = nuevaLista
        notifyDataSetChanged()
    }
}