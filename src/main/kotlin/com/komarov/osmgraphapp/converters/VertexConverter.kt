package com.komarov.osmgraphapp.converters

import com.komarov.osmgraphapp.components.Vertex
import com.komarov.osmgraphapp.entities.LocationEntity
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class VertexConverter: Converter<LocationEntity, Vertex<Long>> {
    override fun convert(source: LocationEntity): Vertex<Long> {
        return Vertex(
            id = source.id,
            lat = source.latitude,
            lon = source.longitude
        )
    }
}