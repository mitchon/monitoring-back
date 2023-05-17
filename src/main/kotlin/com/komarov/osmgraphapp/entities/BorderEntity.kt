package com.komarov.osmgraphapp.entities

import java.util.UUID

data class BorderEntity (
    val id: UUID,
    val fromDistrict: String,
    val toDistrict: String,
    val location: LocationEntity
)

data class BorderInsertableEntity (
    val id: UUID,
    val fromDistrict: String,
    val toDistrict: String,
    val location: Long
)