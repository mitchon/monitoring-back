package com.komarov.osmgraphapp.converters

import com.komarov.osmgraphapp.entities.LocationEntity
import com.komarov.osmgraphapp.models.Location
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class LocationConverter: Converter<LocationEntity, Location> {
    override fun convert(source: LocationEntity): Location {
        return Location(
            id = source.id,
            latitude = source.latitude,
            longitude = source.longitude,
            district = "",
            type = ""
        )
    }
}