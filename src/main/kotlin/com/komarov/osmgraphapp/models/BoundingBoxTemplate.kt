package com.komarov.osmgraphapp.models

data class BoundingBoxTemplate(
    val minLatitude: Double,
    val minLongitude: Double,
    val maxLatitude: Double,
    val maxLongitude: Double
)