package com.mapxushsitp.service

fun String?.safe() = this ?: ""
fun Int?.safe() = this ?: 0
fun <T> List<T>?.safe() = this ?: listOf<T>()
