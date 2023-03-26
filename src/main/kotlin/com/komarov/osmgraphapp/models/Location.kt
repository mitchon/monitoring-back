package com.komarov.osmgraphapp.models

data class Location(
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val links: List<LocationLink> = listOf()
)