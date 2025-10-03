package com.mapxushsitp.service

import java.util.Locale
import kotlin.math.roundToInt

fun Double.roundToNearestHalfString(): String {
    val rounded = (this * 2).roundToInt() / 2.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}

fun Double.toMeterText(locale: Locale): String {
    val rounded = (this * 2).roundToInt() / 2.0
    val whole = rounded.toInt()
    val hasHalf = rounded % 1.0 == 0.5

    return when {
        locale.language == "zh" -> {
            val wholeText = whole.toChineseNumber()

            when {
                whole == 0 && hasHalf -> "半公尺"
                hasHalf && whole > 0 -> "${wholeText}公尺半"
                !hasHalf -> "${wholeText}公尺"
                else -> "${this}公尺" // fallback
            }
        }
        else -> { // English
            when {
                whole == 0 && hasHalf -> "half a meter"
                hasHalf && whole > 0 -> "$whole and half a meter"
                !hasHalf -> if (whole == 1) "1 meter" else "$whole meters"
                else -> "$this meters" // fallback
            }
        }
    }
}

fun Int.toChineseNumber(): String {
    if (this == 0) return "零"

    val digits = arrayOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
    val units = arrayOf("", "十", "百", "千", "萬")

    if (this < 10) return digits[this]
    if (this < 20) return if (this == 10) "十" else "十${digits[this % 10]}"
    if (this < 100) return "${digits[this / 10]}十${if (this % 10 == 0) "" else digits[this % 10]}"

    // Handle larger numbers (basic implementation for common cases)
    var num = this
    var result = ""
    var unitIndex = 0

    while (num > 0) {
        val digit = num % 10
        if (digit != 0) {
            result = digits[digit] + (if (unitIndex > 0) units[unitIndex] else "") + result
        } else if (result.isNotEmpty() && !result.startsWith("零")) {
            result = "零$result"
        }
        num /= 10
        unitIndex++
        if (unitIndex >= units.size) break
    }

    // Clean up consecutive zeros and trailing zeros
    result = result.replace("零+".toRegex(), "零")
        .replace("零$".toRegex(), "")

    return result
}

fun generateSpeakText(heading: String, distance: Double, locale: Locale): String {
    val text = when {
        locale.language == "zh" -> if (distance == 0.0) heading else "${heading} ${distance.toMeterText(locale)}"
        else -> if (distance == 0.0) heading else "${heading} for ${distance.toMeterText(locale)}"
    }
    return text
}

