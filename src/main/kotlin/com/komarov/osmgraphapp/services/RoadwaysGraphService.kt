package com.komarov.osmgraphapp.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.komarov.osmgraphapp.converters.LocationConverter
import com.komarov.osmgraphapp.entities.LocationEntity
import com.komarov.osmgraphapp.entities.LocationLinkEntity
import com.komarov.osmgraphapp.handlers.NodeHandler
import com.komarov.osmgraphapp.handlers.WaysHandler
import com.komarov.osmgraphapp.models.*
import com.komarov.osmgraphapp.repositories.LocationRepository
import com.komarov.osmgraphapp.repositories.LocationLinkRepository
import de.westnordost.osmapi.overpass.OverpassMapDataApi
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RoadwaysGraphService(
    private val overpass: OverpassMapDataApi,
    private val objectMapper: ObjectMapper,
    private val locationRepository: LocationRepository,
    private val locationLinkRepository: LocationLinkRepository,
    private val locationConverter: LocationConverter
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val boundingBox = BoundingBoxTemplate.maxBoundingBox

    fun requestBuild(): RequestResponse {
        locationRepository.deleteAll()
        locationLinkRepository.deleteAll()
        val requestId = UUID.randomUUID()
        logger.info("Started build by request $requestId")
        val roads = requestRoads()
        logger.info("Got all roads by request $requestId")
        val locations = roads.flatMap { it.requestLocations() }
            .groupBy { it.id }.map {
                it.value.first().copy(
                    links = it.value.flatMap { it.links }
                )
            }
        logger.info("Inserting by request $requestId")
        val locationEntities = locations.map {
            LocationEntity.fromModel(it)
        }
        locationRepository.insertBatch(locationEntities)
        val locationLinkEntities = locations.flatMap {
            it.links.map {
                LocationLinkEntity.fromModel(it)
            }
        }
        locationLinkRepository.insertBatch(locationLinkEntities)
        logger.info("Request $requestId is fulfilled")
        return RequestResponse(requestId)
    }

    fun requestGraph(): List<LocationLink> {
        val locations = locationRepository.findAll()
        return locations.flatMap {
            it.links.map { link ->
                LocationLink(
                    start = locations.first { it.id == link.start }.let { locationConverter.convert(it) },
                    finish = locations.first { it.id == link.finish }.let { locationConverter.convert(it) },
                    length = link.length
                )
            }
        }
    }

    private fun Road.requestLocations(): List<Location> {
        val nodeHandler = NodeHandler()
        val locations = this.nodes.map { nodeId ->
            overpass.queryElements(
                """
                    (
                        node($nodeId);
                    );
                    out meta;
                """.trimIndent(),
                nodeHandler
            )
            nodeHandler.get().let { node ->
                Location(
                    id = nodeId,
                    latitude = node.position.latitude,
                    longitude = node.position.longitude
                )
            }
        }
        if (locations.isEmpty()) return listOf()
        val primary = locations.zipWithNext().map { link ->
            val newLink = LocationLink(
                start = link.first,
                finish = link.second,
                length = 0.0
            )
            link.first.copy(
                links = link.first.links + newLink
            )
        } + locations.last()

        var secondary = listOf<Location>()

        if (!this.oneway) {
            secondary = locations.reversed().zipWithNext().map { link ->
                val newLink = LocationLink(
                    start = link.first,
                    finish = link.second,
                    length = 0.0
                )
                link.first.copy(
                    links = link.first.links + newLink
                )
            } + locations.first()
        }

        return (primary + secondary).groupBy { it.id }.map {
            it.value.first().copy(
                links = it.value.flatMap { it.links }
            )
        }
    }

    private fun requestRoads(): List<Road> {
        val waysHandler = WaysHandler()
        overpass.queryElements(
            """
                [bbox:${boundingBox.minLatitude},${boundingBox.minLongitude},${boundingBox.maxLatitude},${boundingBox.maxLongitude}];
                (
                    way['highway' = 'primary'];
                    way['highway' = 'secondary'];
                    way['highway' = 'tertiary'];
                    way['highway' = 'residential'];
                );
                out meta;
            """.trimIndent(),
            waysHandler
        )
        return waysHandler.get().map { way ->
            val oneway = way.tags["oneway"]?.let { it == "yes" } ?: false
            val roundabout = way.tags["junction"]?.let { it == "roundabout" } ?: false
            Road(
                id = way.id,
                nodes = way.nodeIds,
                oneway = oneway || roundabout
            )
        }.distinctBy { it.id }
    }
}