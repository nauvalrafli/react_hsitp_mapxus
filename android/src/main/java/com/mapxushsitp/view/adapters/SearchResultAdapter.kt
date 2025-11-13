package com.mapxushsitp.view.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.mapxushsitp.service.getTranslation
import com.mapxushsitp.R
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiInfo
import java.util.Locale

class SearchResultAdapter(
    private val locale: Locale,
    private val onPoiClick: (PoiInfo, String) -> Unit
) : RecyclerView.Adapter<SearchResultAdapter.SearchResultViewHolder>() {

    private var poiList: List<PoiInfo> = emptyList()

    fun updateList(newList: List<PoiInfo>) {
        poiList = newList
        notifyDataSetChanged()
    }

    class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.result_icon)
        val tvName: TextView = itemView.findViewById(R.id.result_title)
        val tvFloor: TextView = itemView.findViewById(R.id.result_subtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return SearchResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val poi = poiList[position]
        val poiName = poi.nameMap?.getTranslation(locale) ?: ""

        // Set icon based on type
        val iconRes = when {
            poiName.contains("Male Toilet", ignoreCase = true) -> R.drawable.ic_male
            poiName.contains("Female Toilet", ignoreCase = true) -> R.drawable.ic_female
            poiName.contains("Accessible Toilet", ignoreCase = true) -> R.drawable.ic_accessible
            else -> R.drawable.ic_map
        }
        holder.ivIcon.setImageResource(iconRes)
        holder.ivIcon.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.primary_blue))

        holder.tvName.text = poiName
        holder.tvFloor.text = poi.floor + " ${poi.category}"

        holder.itemView.setOnClickListener {
            onPoiClick(poi, poi.poiId)
        }
    }

    override fun getItemCount(): Int = poiList.size
}
