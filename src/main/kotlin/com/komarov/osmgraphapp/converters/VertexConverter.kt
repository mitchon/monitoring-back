package com.komarov.osmgraphapp.converters

import com.komarov.osmgraphapp.components.Edge
import com.komarov.osmgraphapp.components.Vertex
import com.komarov.osmgraphapp.entities.LocationEntity
import com.komarov.osmgraphapp.entities.LocationLinkEntity
import com.komarov.osmgraphapp.repositories.LocationRepository
import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component

@Component
class VertexConverter: Converter<LocationEntity, Vertex<LocationLinkEntity, LocationEntity>> {
    override fun convert(source: LocationEntity): Vertex<LocationLinkEntity, LocationEntity> {
        return Vertex(
            backing = source,
            lat = source.latitude,
            lon = source.longitude
        )
    }
}

@Component
class EdgeConverter(
    private val locationRepository: LocationRepository,
    private val vertexConverter: VertexConverter
): Converter<LocationLinkEntity, Edge<LocationLinkEntity, LocationEntity>> {
    override fun convert(source: LocationLinkEntity): Edge<LocationLinkEntity, LocationEntity> {
        return Edge(
            backing = source,
            start = locationRepository.findById(source.start)?.let {
                vertexConverter.convert(it)
            } ?: throw RuntimeException("Location ${source.start} not found"),
            finish = locationRepository.findById(source.finish)?.let {
                vertexConverter.convert(it)
            } ?: throw RuntimeException("Location ${source.finish} not found"),
            length = source.length
        )
    }
}