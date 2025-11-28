package com.mapxushsitp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapxushsitp.service.getTranslation
import com.mapxushsitp.R
import com.mapxus.map.mapxusmap.api.services.model.building.IndoorBuildingInfo
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiInfo
import java.util.Locale

/**
 * A lightweight, flexible list adapter that mirrors the presentation of the toilet list but
 * can display any category-like items.
 */
class CategoryListAdapter(
    private val buildingList: List<IndoorBuildingInfo>,
    private val locale: Locale,
    private val onItemSelected: (PoiInfo) -> Unit = {}
) : RecyclerView.Adapter<CategoryListAdapter.CategoryViewHolder>() {

    private val items = mutableListOf<PoiInfo>()

    fun submitItems(newItems: List<PoiInfo>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_list_entry, parent, false)
        return CategoryViewHolder(view, buildingList, locale, onItemSelected)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class CategoryViewHolder(
        itemView: View,
        private val buildingList: List<IndoorBuildingInfo>,
        private val locale: Locale,
        private val onItemSelected: (PoiInfo) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val icon: ImageView = itemView.findViewById(R.id.category_item_icon)
        private val title: TextView = itemView.findViewById(R.id.category_item_title)
        private val subtitle: TextView = itemView.findViewById(R.id.category_item_subtitle)

        fun bind(item: PoiInfo) {
            title.text = item.nameMap?.getTranslation(locale = locale)
            subtitle.text = (item.floor ?: item.sharedFloorNames?.getTranslation(locale)) + " - " + buildingList.find { it.buildingId == item.buildingId }?.buildingNamesMap?.getTranslation(locale)
            icon.setImageResource(R.drawable.baseline_location_on_24)

            itemView.setOnClickListener { onItemSelected(item) }
        }
    }
}


