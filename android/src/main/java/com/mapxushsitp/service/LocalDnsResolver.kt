package com.mapxushsitp.service

import okhttp3.Dns
import java.net.InetAddress

class LocalDnsResolver(
  private val localHost: String = DEFAULT_LOCAL_HOST,
  private val localIp: String = DEFAULT_LOCAL_IP
) : Dns {

  override fun lookup(hostname: String): List<InetAddress> {
    if (hostname.equals(localHost, ignoreCase = true)) {
      return listOf(InetAddress.getByName(localIp))
    }

    return Dns.SYSTEM.lookup(hostname)
  }

  companion object {
    const val DEFAULT_LOCAL_HOST = "appapi-uat.hsitp.local"
    const val DEFAULT_LOCAL_IP = "10.211.0.14"
  }
}

