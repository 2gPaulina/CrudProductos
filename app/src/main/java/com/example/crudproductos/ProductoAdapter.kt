package com.example.crudproductos

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class ProductoAdapter(
    private var listaProductos: List<Producto>,
    private val onEditarClick: (Producto) -> Unit,
    private val onEliminarClick: (Producto) -> Unit
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardProducto: CardView = itemView.findViewById(R.id.cardProducto)
        val tvNombreProducto: TextView = itemView.findViewById(R.id.tvNombreProducto)
        val tvPrecio: TextView = itemView.findViewById(R.id.tvPrecio)
        val tvCodigoBarras: TextView = itemView.findViewById(R.id.tvCodigoBarras)
        val tvCategoriaMarca: TextView = itemView.findViewById(R.id.tvCategoriaMarca)
        val tvStock: TextView = itemView.findViewById(R.id.tvStock)
        val btnEditarItem: Button = itemView.findViewById(R.id.btnEditarItem)
        val btnEliminarItem: Button = itemView.findViewById(R.id.btnEliminarItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        val producto = listaProductos[position]

        // 1. Asignar datos del producto a las vistas
        holder.tvNombreProducto.text = producto.nombre
        holder.tvPrecio.text = "$${producto.precio_venta}"
        holder.tvCodigoBarras.text = "Código: ${producto._id}"
        holder.tvCategoriaMarca.text = "Categoría: ${producto.categoria_nombre} | Marca: ${producto.marca_nombre}"
        holder.tvStock.text = "Stock: ${producto.inventario}"

        // 2. Click Listeners (Siempre habilitados para Modificar y Eliminar)
        holder.btnEditarItem.isEnabled = true
        holder.btnEliminarItem.isEnabled = true

        holder.btnEditarItem.setOnClickListener {
            onEditarClick(producto)
        }

        holder.btnEliminarItem.setOnClickListener {
            onEliminarClick(producto)
        }

        // ⚡ 3. LÓGICA VISUAL DE STOCK (Gris / Opaco para agotados)
        if (producto.inventario <= 0) {
            // Opacidad en la tarjeta para dar el aspecto grisáceo/desactivado
            holder.cardProducto.alpha = 0.5f

            // Opcional: También puedes cambiar el fondo de la Card a un color gris claro
            holder.cardProducto.setCardBackgroundColor(Color.parseColor("#E0E0E0"))

            // Aseguramos que los botones sigan viéndose nítidos (100% opacidad)
            holder.btnEditarItem.alpha = 1.0f
            holder.btnEliminarItem.alpha = 1.0f
        } else {
            // Restaurar visualización normal para productos con stock (> 0)
            holder.cardProducto.alpha = 1.0f

            // Restaurar fondo blanco (o el color original de tu Card)
            holder.cardProducto.setCardBackgroundColor(Color.WHITE)

            holder.btnEditarItem.alpha = 1.0f
            holder.btnEliminarItem.alpha = 1.0f
        }
    }

    override fun getItemCount(): Int = listaProductos.size

    fun actualizarLista(nuevaLista: List<Producto>) {
        this.listaProductos = nuevaLista
        notifyDataSetChanged()
    }
}