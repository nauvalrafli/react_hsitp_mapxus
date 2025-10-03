package com.mapxushsitp.data.model

data class Venue(
    val buildingId: String,
    val name: MultilingualText,
    val buildingName: MultilingualText,
    val address: MultilingualAddress,
    val description: Map<String, String>,
    val venueId: String,
    val venueName: MultilingualText,
    val defaultFloor: String,
    val type: String,
    val buildingOutlineId: Long,
    val bbox: Bbox,
    val labelCenter: LabelCenter,
    val country: String,
    val region: String,
    val visualMap: Boolean,
    val signalMap: Boolean,
    val floors: List<Floor>,
    val isPrivate: String,
    val organization: String
) {
    data class MultilingualText(
        val `default`: String?,
        val en: String?,
        val zhHans: String?,
        val zhHant: String?
    )

    data class Address(
        val housenumber: String?,
        val street: String?
    )

    data class MultilingualAddress(
        val `default`: Address?,
        val en: Address?,
        val zhHans: Address?,
        val zhHant: Address?
    )

    data class Bbox(
        val maxLat: Double,
        val maxLon: Double,
        val minLat: Double,
        val minLon: Double
    )

    data class LabelCenter(
        val lat: Double,
        val lon: Double
    )

    data class Floor(
        val id: String,
        val code: String,
        val ordinal: Int,
        val visualMap: Boolean,
        val signalMap: Boolean
    )
}

