package com.komarov.osmgraphapp.models

data class BoundingBoxTemplate(
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double
) {
    companion object {
        val maxBoundingBox = BoundingBoxTemplate(
            minLatitude = 55.751495 - 0.075 / 2,
            minLongitude = 37.618174 - 0.075,
            maxLatitude = 55.751495 + 0.075 / 2,
            maxLongitude = 37.618174 + 0.075,
        )
    }
}