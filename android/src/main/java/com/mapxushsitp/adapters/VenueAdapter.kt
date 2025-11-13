package com.mapxushsitp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapxushsitp.service.getTranslation
import com.mapxushsitp.R
import com.mapxus.map.mapxusmap.api.services.model.building.IndoorBuildingInfo
import com.mapxus.map.mapxusmap.api.services.model.venue.VenueInfo
import java.util.Locale

class VenueAdapter(
    private val locale: Locale = Locale.getDefault(),
    private val onItemClick: (IndoorBuildingInfo) -> Unit,
) : RecyclerView.Adapter<VenueAdapter.VenueViewHolder>() {

    private var venues = listOf<VenueInfo>()
    private var building = listOf<IndoorBuildingInfo>()

    fun updateVenues(venues: List<VenueInfo>) {
        this.venues = venues
        notifyDataSetChanged()
    }

    fun updateBuilding(building: List<IndoorBuildingInfo>) {
        this.building = building
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VenueViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_venue_card, parent, false)
        return VenueViewHolder(view)
    }

    override fun onBindViewHolder(holder: VenueViewHolder, position: Int) {
        val venue = building[position]
        holder.bind(venue)
    }

    override fun getItemCount(): Int = venues.size

    inner class VenueViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val venueName: TextView = itemView.findViewById(R.id.venue_name)
        private val venueAddress: TextView = itemView.findViewById(R.id.venue_address)

        fun bind(venue: IndoorBuildingInfo) {
            venueName.text = venue.nameMap.getTranslation(locale)
            venueAddress.text = venue.addressMap?.getTranslation(locale)?.street ?: ""

            itemView.setOnClickListener {
                onItemClick(venue)
            }
        }
    }

}
