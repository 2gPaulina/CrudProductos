package com.example.crudproductos

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

        holder.tvNombreProducto.text = producto.nombre
        holder.tvPrecio.text = "$${producto.precio_venta}"
        holder.tvCodigoBarras.text = "Código: ${producto._id}"
        holder.tvCategoriaMarca.text = "Categoría: ${producto.categoria_nombre} | Marca: ${producto.marca_nombre}"
        holder.tvStock.text = "Stock: ${producto.inventario}"

        holder.btnEditarItem.setOnClickListener {
            onEditarClick(producto)
        }

        holder.btnEliminarItem.setOnClickListener {
            onEliminarClick(producto)
        }
    }

    override fun getItemCount(): Int = listaProductos.size

    fun actualizarLista(nuevaLista: List<Producto>) {
        this.listaProductos = nuevaLista
        notifyDataSetChanged()
    }
}