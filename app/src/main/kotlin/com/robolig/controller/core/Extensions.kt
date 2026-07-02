package com.robolig.controller.core

import java.util.Locale

fun Int?.asDisplayValue(unit: String = ""): String = this?.let { "$it$unit" } ?: "Unknown"

fun Float?.asSpeedLabel(): String = this?.let { String.format(Locale.US, "%.1f m/s", it) } ?: "Unknown"

fun String?.asFallback(fallback: String = "Not configured"): String = if (this.isNullOrBlank()) fallback else this
