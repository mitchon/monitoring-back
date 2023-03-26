package com.komarov.osmgraphapp.models

data class LocationLink (
    val start: Location,
    val finish: Location,
    val length: Double
)