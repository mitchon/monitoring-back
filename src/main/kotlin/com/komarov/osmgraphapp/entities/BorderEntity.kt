package com.komarov.osmgraphapp.entities

data class BorderEntity (
    val fromDistrict: String,
    val toDistrict: String,
    val location: LocationEntity
)

data class BorderInsertableEntity (
    val fromDistrict: String,
    val toDistrict: String,
    val location: Long
)