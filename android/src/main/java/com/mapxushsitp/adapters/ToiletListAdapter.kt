package com.mapxushsitp.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapxushsitp.data.model.MapPoi
import com.mapxushsitp.service.getTranslation
import com.mapxushsitp.R
import com.mapxus.map.mapxusmap.api.services.BuildingSearch
import com.mapxus.map.mapxusmap.api.services.model.building.IndoorBuildingInfo
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiInfo
import java.util.Locale
import kotlin.coroutines.coroutineContext

class ToiletListAdapter(
    private val buildingList: List<IndoorBuildingInfo>,
    private val locale: Locale = Locale.getDefault(),
    private val onItemClick: (PoiInfo) -> Unit,
) : RecyclerView.Adapter<ToiletListAdapter.ToiletViewHolder>() {

    private var toiletList = listOf<PoiInfo>()

    fun updateToilets(toilets: List<PoiInfo>) {
        toiletList = toilets.filter {
            it.buildingId != null && (it.floorId != null || it.sharedFloorId != null)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToiletViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_toilet, parent, false)
        return ToiletViewHolder(view)
    }

    override fun onBindViewHolder(holder: ToiletViewHolder, position: Int) {
        val toilet = toiletList[position]
        holder.bind(toilet)
    }

    override fun getItemCount(): Int = toiletList.size

    inner class ToiletViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.toilet_icon)
        private val title: TextView = itemView.findViewById(R.id.toilet_title)
        private val subtitle: TextView = itemView.findViewById(R.id.toilet_subtitle)
        private val statusIndicator: View = itemView.findViewById(R.id.status_indicator)
        private val statusText: TextView = itemView.findViewById(R.id.status_text)

        fun bind(toilet: PoiInfo) {
            // TODO: Bind actual data from toilet object
            title.text = toilet.nameMap?.getTranslation(locale)
            subtitle.text = (toilet.floor ?: toilet.sharedFloorNames?.getTranslation(locale)) + " - " + buildingList.find { it.buildingId == toilet.buildingId }?.buildingNamesMap?.getTranslation(locale)
            val random = Math.random() * 3
            if(random > 2) {
                statusText.text = statusText.resources.getString(R.string.full)
                statusIndicator.backgroundTintList = ColorStateList.valueOf(Color.RED)
            } else if(random > 1) {
                statusText.text = statusText.resources.getString(R.string.almost_full)
                statusIndicator.backgroundTintList = ColorStateList.valueOf(Color.YELLOW)
            } else {
                statusText.text = statusText.resources.getString(R.string.available)
                statusIndicator.backgroundTintList = ColorStateList.valueOf(Color.BLUE)
            }

            itemView.setOnClickListener {
                onItemClick(toilet)
            }
        }
    }
}
