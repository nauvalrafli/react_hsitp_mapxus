package com.mapxushsitp.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.io.Serializable

data class PathDto(
    val distance: Double,
    val time: Long,
    val bbox: List<Double>,
//    val points: LineString?, // didnt need it
    val indoorPoints: List<IndoorLatLng>,
    val requestPoint: List<RoutePlanningPoint>,
    val instructions: List<InstructionDto>
)

data class IndoorLatLng(
    val lat: Double,
    val lon: Double,
    val floorId: String,
    val buildingId: String?
)

data class RoutePlanningPoint(
    val lon: Double,
    val lat: Double,
    val floorId: String
)

data class InstructionDto(
    val venueId: String,
    val ordinal: String,
    val buildingId: String,
    val distance: Double,
    val heading: Double,
    val sign: Int,
    val interval: List<Int>,
    val indoorPoints: List<IndoorLatLng>,
    val text: String,
    val time: Long,
    val floorId: String,
    val type: String?, // Assuming it can be null
    val streetName: String
)

data class SerializableRoutePoint(
    val lat: Double,
    val lon: Double,
    val floorId: String
) : Serializable

data class SerializableNavigationInstruction(
    val instruction: String,
    val distance: Double,
    val floorId: String
) : Serializable

data class SerializableRouteInstruction(
    val instruction: String,
    val distance: Double,
    val floorId: String
) : Serializable

@Parcelize
data class ParcelizeRoutePoint(
    val lat: Double,
    val lon: Double,
    val floorId: String
) : Parcelable


