package com.mapxushsitp.data.api

import com.google.gson.annotations.SerializedName

data class SheetsValuesResponse(
  @SerializedName("range") val range: String? = null,
  @SerializedName("majorDimension") val majorDimension: String? = null,
  @SerializedName("values") val values: List<List<String>>? = null
) {
  /*
  * This sheet uses these column : DeviceId, building, floor, toiletName, MapxusId, Mapxus Type, Mapxus BuildingId, Mapxus FloorId
  *
  * */




  /*
  * 0 -> vacant
  * 1 -> half-occupied
  * 2 -> Full
  * */
  fun getDeviceIds(
    buildingId: String,
    floorId: String,
    poiId: String
  ) : List<String> {
    val result = arrayListOf<String>()
    this.values?.filter { row ->
      row[6] == buildingId && row[7] == floorId && row[4] == poiId
    }?.forEach { row ->
      result.add(row[0])
    }
    return result
  }

  fun getDevicesFromBuildingId(buildingId: String?) : List<String> {
    val result = arrayListOf<String>()
    this.values?.filter { row ->
      row[6] == buildingId
    }?.forEach { row ->
      result.add(row[0])
    }
    return result
  }

  fun getDevicesMappingFromBuildingId(buildingId: String?): Map<String, List<String>> {
    return this.values
      ?.filter { row -> row[6] == buildingId } // Row 6 is BuildingId based on your sheet
      ?.groupBy(
        keySelector = { row -> row[4] },   // MapxusId (Key)
        valueTransform = { row -> row[0] } // DeviceId (Value)
      ) ?: emptyMap()
  }
}

