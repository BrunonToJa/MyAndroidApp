package com.example.mybussines.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.mybussines.R

class PositionAdapter(
    private val items: MutableList<PositionItem>,
    private val onItemClick: (PositionItem, Int) -> Unit
) : RecyclerView.Adapter<PositionAdapter.ViewHolder>() {

    data class PositionItem(
        val name: String,
        val openDate: String,
        val closeDate: String,
        val openPrice: Float,
        val closePrice: Float,
        val accountType: String,
        val isStillOpen: Boolean
    )

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.cardPosition)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvDates: TextView = view.findViewById(R.id.tvDates)
        val tvValues: TextView = view.findViewById(R.id.tvValues)
        val tvProfit: TextView = view.findViewById(R.id.tvProfit)
        val vStatus: View = view.findViewById(R.id.vStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_position, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvName.text = item.name
        holder.tvDates.text = if (item.isStillOpen) {
            "${item.openDate}\n- Otwarte"
        } else {
            "${item.openDate}\n- ${item.closeDate}"
        }
        holder.tvValues.text = if (item.isStillOpen) {
            "${item.openPrice.toInt()}\n${item.closePrice.toInt()}"
        } else {
            "${item.openPrice.toInt()}\n${item.closePrice.toInt()}"
        }

        if (item.isStillOpen) {
            holder.tvProfit.text = "BD"
            holder.tvProfit.setTextColor(0xFFB0B0B0.toInt())
            holder.vStatus.setBackgroundResource(R.drawable.circle_green)
        } else {
            val profitPercent = if (item.openPrice > 0) {
                ((item.closePrice - item.openPrice) / item.openPrice * 100)
            } else 0f
            val profitText = "${if (profitPercent >= 0) "+" else ""}${"%.2f".format(profitPercent)}%"
            holder.tvProfit.text = profitText
            holder.tvProfit.setTextColor(
                if (profitPercent >= 0) 0xFF10B981.toInt() else 0xFFEF4444.toInt()
            )
            holder.vStatus.setBackgroundResource(
                if (profitPercent >= 0) R.drawable.circle_green else R.drawable.circle_red
            )
        }

        holder.card.setOnClickListener {
            onItemClick(item, position)
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newList: List<PositionItem>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    fun updateItem(index: Int, newItem: PositionItem) {
        items[index] = newItem
        notifyItemChanged(index)
    }

    fun removeItem(index: Int) {
        items.removeAt(index)
        notifyItemRemoved(index)
    }

    fun getItems(): List<PositionItem> = items.toList()
}