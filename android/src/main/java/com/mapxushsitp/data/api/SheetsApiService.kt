package com.mapxushsitp.data.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SheetsApiService {

  @GET("v4/spreadsheets/{spreadsheetId}/values/{range}")
  suspend fun getValues(
    @Path("spreadsheetId") spreadsheetId: String,
    @Path("range", encoded = true) range: String,
    @Query("key") apiKey: String
  ): SheetsValuesResponse
}

