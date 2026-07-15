package com.robolig.controller.domain.model

/**
 * A single cube detection result, in original frame pixel coordinates.
 *
 * Coordinates are in the source image space, not the displayed preview space,
 * so the UI scales by the display ratio when drawing.
 */
data class CubeDetection(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val centroidX: Float,
    val centroidY: Float,
    val areaPx: Int,
    val aspectRatio: Float,
) {
    val right: Int get() = x + width
    val bottom: Int get() = y + height
}
