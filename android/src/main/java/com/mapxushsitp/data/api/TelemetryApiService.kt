package com.mapxushsitp.data.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface TelemetryApiService {

  @GET("telemetry/plugins/telemetry/DEVICE/{deviceId}/values/timeseries")
  suspend fun getDeviceTelemetry(
    @Header("Authorization") authorization: String,
    @Path("deviceId") deviceId: String,
    @Query("useStrictDataTypes") useStrictDataTypes: Boolean = false
  ): DeviceTelemetryResponse
}

