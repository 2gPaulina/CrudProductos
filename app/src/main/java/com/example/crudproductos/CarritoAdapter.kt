package com.example.crudproductos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CarritoAdapter(
    private val items: List<ItemCarrito>
) : RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder>() {

    class CarritoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreItem)
        val tvDetalle: TextView = itemView.findViewById(R.id.tvDetalleItem)
        val tvSubtotal: TextView = itemView.findViewById(R.id.tvSubtotalItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarritoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carrito, parent, false)
        return CarritoViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarritoViewHolder, position: Int) {
        val item = items[position]
        val subtotal = item.producto.precio_venta * item.cantidad

        holder.tvNombre.text = item.producto.nombre
        holder.tvDetalle.text = "${item.cantidad} x $${String.format("%.2f", item.producto.precio_venta)}"
        holder.tvSubtotal.text = "$${String.format("%.2f", subtotal)}"
    }

    override fun getItemCount(): Int = items.size
}