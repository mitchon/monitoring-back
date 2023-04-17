package com.komarov.osmgraphapp.entities

import com.komarov.osmgraphapp.models.Location
import com.komarov.osmgraphapp.models.LocationLink

data class LocationEntity (
    val id: Long,
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        fun fromModel(source: Location): LocationEntity {
            return LocationEntity(
                id = source.id,
                latitude = source.latitude,
                longitude = source.longitude
            )
        }
    }
}

data class LocationLinkEntity (
    val start: LocationEntity,
    val finish: LocationEntity,
    val length: Double
)

data class LocationLinkInsertableEntity (
    val start:  Long,
    val finish: Long,
    val length: Double
) {
    companion object {
        fun fromModel(model: LocationLink): LocationLinkInsertableEntity {
            return LocationLinkInsertableEntity(
                start = model.start.id,
                finish = model.finish.id,
                length = model.length
            )
        }
    }
}