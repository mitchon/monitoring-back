package com.komarov.osmgraphapp.models

data class BoundingBoxTemplate(
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double
) {
    companion object {
        val maxBoundingBox = BoundingBoxTemplate(
            minLatitude = 54.7585694 - 0.07 / 2,
            minLongitude = 38.8818137 - 0.07,
            maxLatitude = 54.7585694 + 0.07 / 2,
            maxLongitude = 38.8818137 + 0.07,
        )
    }
}