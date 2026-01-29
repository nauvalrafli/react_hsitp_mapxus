package com.mapxushsitp.service

import com.mapxushsitp.data.api.AuthApiService
import com.mapxushsitp.data.api.SheetsApiService
import com.mapxushsitp.data.api.TelemetryApiService
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

  private const val DEFAULT_BASE_URL = "http://appapi-uat.hsitp.local/"
  private const val GOOGLE_SHEETS_BASE_URL = "https://sheets.googleapis.com/"

  private val defaultOkHttpClient: OkHttpClient by lazy {
    buildOkHttpClient(LocalDnsResolver())
  }

  private val defaultRetrofit: Retrofit by lazy {
    buildRetrofit(DEFAULT_BASE_URL, defaultOkHttpClient)
  }

  val authApiService: AuthApiService by lazy {
    defaultRetrofit.create(AuthApiService::class.java)
  }

  fun telemetryService(
    baseUrl: String = DEFAULT_BASE_URL,
    localHost: String = LocalDnsResolver.DEFAULT_LOCAL_HOST,
    localIp: String = LocalDnsResolver.DEFAULT_LOCAL_IP
  ): TelemetryApiService {
    val resolver = LocalDnsResolver(localHost, localIp)
    val retrofit = buildRetrofit(baseUrl, buildOkHttpClient(resolver))
    return retrofit.create(TelemetryApiService::class.java)
  }

  fun sheetsService(): SheetsApiService {
    // Google Sheets is a public HTTPS host; no local DNS resolver needed.
    val retrofit = buildRetrofit(GOOGLE_SHEETS_BASE_URL, OkHttpClient.Builder().dispatcher(buildDispatcher()).build())
    return retrofit.create(SheetsApiService::class.java)
  }

  fun authService(
    baseUrl: String = DEFAULT_BASE_URL,
    localHost: String = LocalDnsResolver.DEFAULT_LOCAL_HOST,
    localIp: String = LocalDnsResolver.DEFAULT_LOCAL_IP
  ): AuthApiService {
    if (baseUrl == DEFAULT_BASE_URL &&
      localHost == LocalDnsResolver.DEFAULT_LOCAL_HOST &&
      localIp == LocalDnsResolver.DEFAULT_LOCAL_IP
    ) {
      return authApiService
    }

    val resolver = LocalDnsResolver(localHost, localIp)
    val retrofit = buildRetrofit(baseUrl, buildOkHttpClient(resolver))
    return retrofit.create(AuthApiService::class.java)
  }

  private fun buildOkHttpClient(resolver: LocalDnsResolver): OkHttpClient {
    return OkHttpClient.Builder()
      .dns(resolver)
      .dispatcher(buildDispatcher())
      .build()
  }

  private fun buildDispatcher(): Dispatcher {
    return Dispatcher().apply {
      maxRequestsPerHost = 150
      // Keep total maxRequests >= per-host to actually allow that concurrency.
      maxRequests = 300
    }
  }


  private fun buildRetrofit(baseUrl: String, okHttpClient: OkHttpClient): Retrofit {
    return Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(okHttpClient)
      .addConverterFactory(GsonConverterFactory.create())
      .build()
  }
}
