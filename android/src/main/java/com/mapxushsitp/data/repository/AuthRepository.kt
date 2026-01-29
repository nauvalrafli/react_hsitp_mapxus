package com.mapxushsitp.data.repository

import com.mapxushsitp.data.api.AuthApiService
import com.mapxushsitp.service.RetrofitClient

class AuthRepository(
  private val service: AuthApiService = RetrofitClient.authService()
) {
  suspend fun fetchToken(
    grantType: String,
    clientId: String,
    clientSecret: String
  ) = service.getToken(grantType, clientId, clientSecret)
}

