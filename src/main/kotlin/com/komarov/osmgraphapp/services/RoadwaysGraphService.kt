package com.komarov.osmgraphapp.services

import com.komarov.osmgraphapp.components.AStarAlgorithm
import com.komarov.osmgraphapp.components.EuclideanDistance
import com.komarov.osmgraphapp.components.GraphAbstractAlgorithm
import com.komarov.osmgraphapp.components.Vertex
import com.komarov.osmgraphapp.converters.EdgeConverter
import com.komarov.osmgraphapp.converters.LocationConverter
import com.komarov.osmgraphapp.converters.VertexConverter
import com.komarov.osmgraphapp.entities.LocationEntity
import com.komarov.osmgraphapp.entities.LocationLinkEntity
import com.komarov.osmgraphapp.handlers.NodesHandler
import com.komarov.osmgraphapp.handlers.WaysHandler
import com.komarov.osmgraphapp.models.*
import com.komarov.osmgraphapp.repositories.LocationRepository
import com.komarov.osmgraphapp.repositories.LocationLinkRepository
import de.westnordost.osmapi.overpass.OverpassMapDataApi
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import kotlin.math.*

@Service
class RoadwaysGraphService(
    private val overpass: OverpassMapDataApi,
    private val locationRepository: LocationRepository,
    private val locationLinkRepository: LocationLinkRepository,
    private val locationConverter: LocationConverter,
    private val vertexConverter: VertexConverter,
    private val edgeConverter: EdgeConverter
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val boundingBox = BoundingBoxTemplate.maxBoundingBox

    fun getRoute(from: Long, to: Long): List<LocationLink> {
        val start = locationRepository.findById(from)?.let {
            vertexConverter.convert(it)
        }
        val goal = locationRepository.findById(to)?.let {
            vertexConverter.convert(it)
        }
        if (start == null || goal == null)
            throw RuntimeException("BadArgumentException, from or to locations are not found")
        val heuristic = EuclideanDistance()
        val algorithm = AStarAlgorithm<LocationLinkEntity, LocationEntity>(heuristic)
        val route = algorithm.getRoute(start, goal) { current ->
            locationLinkRepository.findByStartId(current.backing.id).map {
                edgeConverter.convert(it)
            }
        }
        return route?.map {
            val link = it.backing
            LocationLink(
                start = locationRepository.findById(link.start)!!.let { locationConverter.convert(it) },
                finish = locationRepository.findById(link.finish)!!.let { locationConverter.convert(it) },
                length = link.length
            )
        } ?: throw RuntimeException("Route not found")
    }

    fun requestBuild(): RequestResponse {
        val requestId = UUID.randomUUID()
        if (locationRepository.findAll().isNotEmpty())
            return RequestResponse(requestId)
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
        val nodesHandler = NodesHandler()
        val nodes: MutableMap<Long, Location?> = this.nodes.associateWith { null }.toMutableMap()
        overpass.queryElements(
            """
                (
                    ${nodes.keys.joinToString("\n") { "node($it);" }}
                );
                out meta;
            """.trimIndent(),
            nodesHandler
        )
        nodesHandler.get().map { node ->
            val location = Location(
                id = node.id,
                latitude = node.position.latitude,
                longitude = node.position.longitude
            )
            nodes[location.id] = location
        }
        val locations = nodes.values.filterNotNull()
        if (locations.isEmpty()) return listOf()
        val primary = locations.zipWithNext().map { link ->
            val length = acos(
                sin(link.first.latitude)*sin(link.second.latitude)+
                    cos(link.first.latitude)*cos(link.second.latitude)*cos(link.second.longitude-link.first.longitude)
            ) * 6371
            val newLink = LocationLink(
                start = link.first,
                finish = link.second,
                length = length
            )
            link.first.copy(
                links = link.first.links + newLink
            )
        } + locations.last()

        var secondary = listOf<Location>()

        if (!this.oneway) {
            secondary = locations.reversed().zipWithNext().map { link ->
                val length = acos(
                    sin(link.first.latitude)*sin(link.second.latitude)+
                        cos(link.first.latitude)*cos(link.second.latitude)*cos(link.second.longitude-link.first.longitude)
                ) * 6371
                val newLink = LocationLink(
                    start = link.first,
                    finish = link.second,
                    length = length
                )
                link.first.copy(
                    links = link.first.links + newLink
                )
            } + locations.first()
        }

        logger.info("Got all nodes for way ${this.id}")
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
                    way['highway' = 'living_street'];
                    way['highway' = 'unclassified'];
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