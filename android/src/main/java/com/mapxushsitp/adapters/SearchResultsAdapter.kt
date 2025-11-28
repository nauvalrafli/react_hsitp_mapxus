package com.mapxushsitp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapxushsitp.service.getTranslation
import com.mapxushsitp.R
import com.mapxus.map.mapxusmap.api.services.BuildingSearch
import com.mapxus.map.mapxusmap.api.services.model.BuildingSearchOption
import com.mapxus.map.mapxusmap.api.services.model.building.IndoorBuildingInfo
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiInfo
import java.util.Locale

class SearchResultsAdapter(
    private val locale: Locale,
    private val onItemClick: (PoiInfo) -> Unit
) : RecyclerView.Adapter<SearchResultsAdapter.SearchResultViewHolder>() {

    private var searchResults = listOf<PoiInfo>()
    var buildings = listOf<IndoorBuildingInfo>()

    init {
        val search = BuildingSearch.newInstance()
        search.searchBuildingByOption(BuildingSearchOption().apply {
            this.pageCapacity(20)
        }) {
            buildings = it.indoorBuildingList
        }
    }

    fun updateResults(results: List<PoiInfo>) {
        searchResults = results
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_result, parent, false)
        return SearchResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        val result = searchResults[position]
        holder.bind(result)
    }

    override fun getItemCount(): Int = searchResults.size

    inner class SearchResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.result_icon)
        private val title: TextView = itemView.findViewById(R.id.result_title)
        private val subtitle: TextView = itemView.findViewById(R.id.result_subtitle)


        fun bind(result: PoiInfo) {
            title.text = result.nameMap?.getTranslation(locale = locale) ?: ""
            val buildingName = buildings.firstOrNull { result.buildingId == it.buildingId }
                ?.buildingNamesMap?.getTranslation(locale) ?: ""
            subtitle.text = if (buildingName.isNotEmpty()) {
                "${result.floor} - $buildingName"
            } else {
                result.floor
            }

            itemView.setOnClickListener {
                onItemClick(result)
            }
        }
    }
}
