package com.komarov.osmgraphapp.converters

import com.komarov.osmgraphapp.entities.LocationLinkEntity
import com.komarov.osmgraphapp.models.LocationLink
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class LocationLinkConverter(
    private val locationConverter: LocationConverter
): Converter<LocationLinkEntity, LocationLink> {
    override fun convert(source: LocationLinkEntity): LocationLink {
        return LocationLink(
            start = locationConverter.convert(source.start),
            finish = locationConverter.convert(source.finish),
            length = source.length
        )
    }
}