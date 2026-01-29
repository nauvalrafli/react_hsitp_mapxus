package com.mapxushsitp.data.api

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface AuthApiService {
  @FormUrlEncoded
  @POST("auth/realms/hsitp/protocol/openid-connect/token")
  suspend fun getToken(
    @Field("grant_type") grantType: String,
    @Field("client_id") clientId: String,
    @Field("client_secret") clientSecret: String
  ): TokenResponse
}
