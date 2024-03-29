package com.komarov.osmgraphapp.entities

import com.komarov.osmgraphapp.models.Location
import com.komarov.osmgraphapp.models.LocationLink
import kotlin.math.max

data class LocationEntity (
    val id: Long,
    val latitude: Double,
    val longitude: Double,
    val district: String,
    val type: String
) {
    companion object {
        fun fromModel(source: Location): LocationEntity {
            return LocationEntity(
                id = source.id,
                latitude = source.latitude,
                longitude = source.longitude,
                district = source.district,
                type = source.type
            )
        }
    }
}