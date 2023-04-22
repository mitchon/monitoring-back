package com.komarov.osmgraphapp.entities

import com.komarov.osmgraphapp.models.LocationLink

data class LocationLinkWithLocationsEntity (
    val start: LocationEntity,
    val finish: LocationEntity,
    val length: Double,
    val maxSpeed: Double
)

data class LocationLinkWithFinishEntity (
    val start: Long,
    val finish: LocationEntity,
    val length: Double,
    val maxSpeed: Double
)

data class LocationLinkWithFinishAndStatusEntity (
    val start: Long,
    val finish: LocationEntity,
    val length: Double,
    val maxSpeed: Double,
    val needsReload: Boolean
)

data class LocationLinkInsertableEntity (
    val start:  Long,
    val finish: Long,
    val length: Double,
    val maxSpeed: Double
) {
    companion object {
        fun fromModel(model: LocationLink): LocationLinkInsertableEntity {
            return LocationLinkInsertableEntity(
                start = model.start.id,
                finish = model.finish.id,
                length = model.length,
                maxSpeed = model.maxSpeed
            )
        }
    }
}