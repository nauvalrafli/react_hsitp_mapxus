package com.mapxushsitp.data.api

import com.google.gson.annotations.SerializedName

data class DeviceTelemetryResponse(
  @SerializedName("updatedAt") val updatedAt: List<ValueEntry>? = null,
  @SerializedName("Presence_Seneor_State") val presenceSensorState : List<ValueEntry>? = null
) {
  //    0 is Vacant, 1 is anything but vacant,
  fun isVacant() : Int {
    if(this.presenceSensorState?.isNotEmpty() == true && this.presenceSensorState[0].value != "Vacant")
      return 1
    return 0
  }
}

data class ValueEntry(
  @SerializedName("ts") val ts: Long,
  @SerializedName("value") val value: String
)
