package com.example.crudproductos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TicketAdapter(
    private val items: List<ItemCarrito>
) : RecyclerView.Adapter<TicketAdapter.TicketViewHolder>() {

    class TicketViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCantidad: TextView = itemView.findViewById(R.id.tvCantidadItemTicket)
        val tvNombre: TextView = itemView.findViewById(R.id.tvNombreItemTicket)
        val tvIvaBadge: TextView = itemView.findViewById(R.id.tvIvaBadgeTicket)
        val tvTotal: TextView = itemView.findViewById(R.id.tvTotalItemTicket)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ticket, parent, false)
        return TicketViewHolder(view)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        val item = items[position]
        val totalItem = item.producto.precio_venta * item.cantidad

        holder.tvCantidad.text = "${item.cantidad}x"
        holder.tvNombre.text = item.producto.nombre

        // 👈 AQUÍ TAMBIÉN USAMOS LA FUNCIÓN DE TASA
        holder.tvIvaBadge.text = if (item.producto.obtenerTasaIva() > 0) "(16%)" else "(0%)"
        holder.tvTotal.text = "$${String.format("%.2f", totalItem)}"
    }

    override fun getItemCount(): Int = items.size
}