package com.example.crudproductos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductoUsuarioAdapter(
    private var listaProductos: List<Producto>,
    private val onAgregarClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoUsuarioAdapter.ProductoViewHolder>() {

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombreProducto: TextView = itemView.findViewById(R.id.tvNombreProducto)
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
        val producto = listaProductos[position]

        holder.tvNombreProducto.text = producto.nombre
        holder.tvPrecio.text = "$${String.format("%.2f", producto.precio_venta)}"
        holder.tvExistencias.text = "Stock: ${producto.inventario}"

        // Deshabilitar botón si no hay stock
        holder.btnVender.isEnabled = producto.inventario > 0

        holder.btnVender.setOnClickListener {
            onAgregarClick(producto)
        }
    }

    override fun getItemCount(): Int = listaProductos.size

    fun actualizarLista(nuevaLista: List<Producto>) {
        this.listaProductos = nuevaLista
        notifyDataSetChanged()
    }
}