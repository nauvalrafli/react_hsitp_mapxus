package com.mapxushsitp.adapters

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapxus.map.mapxusmap.api.services.model.building.IndoorBuildingInfo
import com.mapxus.map.mapxusmap.api.services.model.poi.PoiInfo
import com.mapxushsitp.R
import com.mapxushsitp.service.getTranslation
import java.util.Locale

data class ToiletPoi(
  val poiInfo: PoiInfo,
  val occupancy: Double
)

class ToiletListAdapter(
  private val buildingList: List<IndoorBuildingInfo>,
  private val locale: Locale = Locale.getDefault(),
  private val onItemClick: (ToiletPoi) -> Unit,
) : RecyclerView.Adapter<ToiletListAdapter.ToiletViewHolder>() {
    private var toiletOccupancyList = listOf<ToiletPoi>()

    fun updateToilets(toilets: List<ToiletPoi>) {
        toiletOccupancyList = toilets.filter { (poiInfo, occupancy) ->
          poiInfo.buildingId != null && (poiInfo.floorId != null || poiInfo.sharedFloorId != null)
        }
        android.os.Handler(Looper.getMainLooper()).post {
          notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ToiletViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_toilet, parent, false)
        return ToiletViewHolder(view)
    }

    override fun onBindViewHolder(holder: ToiletViewHolder, position: Int) {
        val toilet = toiletOccupancyList[position]
        holder.bind(toilet)
    }

    override fun getItemCount(): Int = toiletOccupancyList.size

    inner class ToiletViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.toilet_icon)
        private val title: TextView = itemView.findViewById(R.id.toilet_title)
        private val subtitle: TextView = itemView.findViewById(R.id.toilet_subtitle)
        private val statusIndicator: View = itemView.findViewById(R.id.status_indicator)
        private val statusText: TextView = itemView.findViewById(R.id.status_text)

        fun bind(toilet: ToiletPoi) {
            // TODO: Bind actual data from toilet object
            title.text = toilet.poiInfo.nameMap?.en
            subtitle.text = (toilet.poiInfo.floor ?: toilet.poiInfo.sharedFloorNames?.getTranslation(locale)) + " - " + buildingList.find { it.buildingId == toilet.poiInfo.buildingId }?.buildingNamesMap?.getTranslation(locale)
            val occ = toilet.occupancy
            if(occ > 75) {
                statusText.text = statusText.resources.getString(R.string.full)
                statusIndicator.backgroundTintList = ColorStateList.valueOf(Color.RED)
            } else if(occ > 25) {
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
