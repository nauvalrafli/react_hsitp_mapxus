package com.mapxushsitp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.ContextCompat
import com.mapxushsitp.R

class CategoryAdapter(
    private val items: List<CategoryItem>
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    data class CategoryItem(
        val name: String,
        @DrawableRes val iconResId: Int,
        @ColorInt val backgroundColor: Int,
        val onItemClick: () -> Unit
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val iconBackground: CardView =
            itemView.findViewById(R.id.category_icon_background)
        private val iconView: ImageView = itemView.findViewById(R.id.category_icon)
        private val labelView: TextView = itemView.findViewById(R.id.category_label)

        fun bind(item: CategoryItem) {
            labelView.text = item.name
            iconView.setImageResource(item.iconResId)
            iconBackground.setCardBackgroundColor(item.backgroundColor)
            iconView.imageTintList = ContextCompat.getColorStateList(
                iconView.context,
                R.color.primary_blue
            )
            itemView.setOnClickListener { item.onItemClick() }
            iconView.setOnClickListener { item.onItemClick() }
        }
    }
}

