package com.robolig.controller.utils

fun hzToPeriodMillis(frequencyHz: Int): Long = 1_000L / frequencyHz.coerceAtLeast(1)
