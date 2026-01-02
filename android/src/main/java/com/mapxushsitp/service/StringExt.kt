package com.mapxushsitp.service

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.mapxushsitp.data.model.Venue
import com.mapxus.map.mapxusmap.api.common.MultilingualObject
import com.mapxus.map.mapxusmap.api.services.model.building.Address
import java.util.Locale

fun Double.limitDecimal(limit: Int = 8): String {
    return String.format("%.${limit}f", this)
}

fun MultilingualObject<String>.getTranslation(locale: Locale): String {
    return when {
        locale.language == "zh" && (locale.country.equals("TW", true) || locale.country.equals("HK", true)) ->
            this.zhHant ?: this.zhHans ?: this.en ?: this.default
        locale.language == "zh" ->
            this.zhHans ?: this.zhHant ?: this.en ?: this.default
        else ->
            this.en ?: this.default
    }
}

fun MultilingualObject<Address>.getTranslation(locale: Locale): Address {
    return when {
        locale.language == "zh" && (locale.country.equals("TW", true) || locale.country.equals("HK", true)) ->
            this.zhHant ?: this.zhHans ?: this.en ?: this.default
        locale.language == "zh" ->
            this.zhHans ?: this.zhHant ?: this.en ?: this.default
        else ->
            this.en ?: this.default
    }
}


fun Venue.MultilingualText.getTranslation(locale: Locale): String {
    return when {
        locale.language == "zh" && (locale.country.equals("TW", true) || locale.country.equals("HK", true)) ->
            this.zhHant ?: this.zhHans ?: this.en ?: this.default ?: ""
        locale.language == "zh" ->
            this.zhHans ?: this.zhHant ?: this.en ?: this.default ?: ""
        else ->
            this.en ?: this.default ?: ""
    }
}

fun Venue.MultilingualAddress.getTranslation(locale: Locale): String {
    return when {
        locale.language == "zh" && (locale.country.equals("TW", true) || locale.country.equals("HK", true)) ->
            this.zhHant?.street ?: this.zhHans?.street ?: this.en?.street ?: this.default?.street ?: ""
        locale.language == "zh" ->
            this.zhHans?.street ?: this.zhHant?.street ?: this.en?.street ?: this.default?.street ?: ""
        else ->
            this.en?.street ?: this.default?.street ?: ""
    }
}


@Composable
fun DynamicStringResource(key: String, fallback: String = key): String {
    val context = LocalContext.current
    val resId = context.resources.getIdentifier(key, "string", context.packageName)
    return if (resId != 0) {
        try {
            context.getString(resId)
        } catch (e: Exception) {
            fallback
        }
    } else {
        fallback
    }
}
