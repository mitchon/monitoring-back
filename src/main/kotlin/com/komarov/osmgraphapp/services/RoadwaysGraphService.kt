package com.komarov.osmgraphapp.services

import com.komarov.osmgraphapp.converters.LocationConverter
import com.komarov.osmgraphapp.converters.LocationLinkConverter
import com.komarov.osmgraphapp.entities.LocationEntity
import com.komarov.osmgraphapp.entities.LocationLinkInsertableEntity
import com.komarov.osmgraphapp.handlers.NodesHandler
import com.komarov.osmgraphapp.handlers.WaysHandler
import com.komarov.osmgraphapp.models.*
import com.komarov.osmgraphapp.repositories.LocationRepository
import com.komarov.osmgraphapp.repositories.LocationLinkRepository
import de.westnordost.osmapi.overpass.OverpassMapDataApi
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.lang.Character.isDigit
import java.util.UUID
import java.util.concurrent.CompletableFuture
import kotlin.math.*

@Service
class RoadwaysGraphService(
    private val overpass: OverpassMapDataApi,
    private val locationRepository: LocationRepository,
    private val locationLinkRepository: LocationLinkRepository,
    private val locationConverter: LocationConverter,
    private val locationLinkConverter: LocationLinkConverter
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val boundingBox = BoundingBoxTemplate.maxBoundingBox

    private var task = CompletableFuture<RequestResponse>()

    private fun requestBuildBody(): RequestResponse {
        val requestId = UUID.randomUUID()
        if (locationRepository.findAll().isNotEmpty())
            return RequestResponse(requestId)
        logger.info("Started build by request $requestId")
        val roads = requestRoads()
        logger.info("Got all roads by request $requestId")
        logger.info(roads.map { it.maxSpeed }.distinct().joinToString(","))
        val locationsWithLinks = roads.stream().map { it.requestLocations() }.toList()
        val locations = locationsWithLinks.flatMap { it.first }.distinctBy { it.id }
        val links = locationsWithLinks.flatMap { it.second }.distinctBy { (it.start.id to it.finish.id) }
        logger.info("Inserting by request $requestId")
        val locationEntities = locations.map {
            LocationEntity.fromModel(it)
        }
        locationRepository.insertBatch(locationEntities)
        val locationLinkEntities = links.map {
            LocationLinkInsertableEntity.fromModel(it)
        }
        locationLinkRepository.insertBatch(locationLinkEntities)
        logger.info("Request $requestId is fulfilled")
        return RequestResponse(requestId)
    }

    fun requestBuild(): RequestResponse {
        if (task.isDone)
            return task.get()
        task.complete(requestBuildBody())
        return task.get()
    }

    fun requestGraph(): List<LocationLink> {
        val links = locationLinkRepository.findAll()
        return links.map {
            locationLinkConverter.convert(it)
        }
    }

    private fun Road.requestLocations(): Pair<List<Location>, List<LocationLink>> {
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
        if (locations.isEmpty()) return (listOf<Location>() to listOf<LocationLink>())
        val primaryDirLinks = locations.zipWithNext().map { link ->
            val length = countDistance(link.first, link.second)
            LocationLink(
                start = link.first,
                finish = link.second,
                length = length,
                maxSpeed = countSpeed(this.maxSpeed)
            )
        }

        var secondaryDirLinks = listOf<LocationLink>()

        if (!this.oneway) {
            secondaryDirLinks = locations.reversed().zipWithNext().map { link ->
                val length = countDistance(link.first, link.second)
                LocationLink(
                    start = link.first,
                    finish = link.second,
                    length = length,
                    maxSpeed = countSpeed(this.maxSpeed)
                )
            }
        }

        logger.info("Got all nodes for way ${this.id}")
        return (locations to primaryDirLinks + secondaryDirLinks)
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
            val maxSpeed = way.tags["maxspeed"]
            Road(
                id = way.id,
                nodes = way.nodeIds,
                oneway = oneway || roundabout,
                maxSpeed = maxSpeed
            )
        }.distinctBy { it.id }
    }

    private fun countDistance(a: Location, b: Location): Double {
        val earthRadius = 6371000.0 // in meters
        val latDiff = Math.toRadians(b.latitude - a.latitude)
        val lonDiff = Math.toRadians(b.longitude - a.longitude)
        val aLat = Math.toRadians(a.latitude)
        val bLat = Math.toRadians(b.latitude)
        val sinLat = sin(latDiff / 2)
        val sinLon = sin(lonDiff / 2)
        val a1 = sinLat * sinLat + cos(aLat) * cos(bLat) * sinLon * sinLon
        val a2 = 2 * atan2(sqrt(a1), sqrt(1 - a1))
        return earthRadius * a2
    }

    private fun countSpeed(tagValue: String?): Double {
        return if ( !tagValue.isNullOrEmpty() && tagValue.all { isDigit(it) } )
            tagValue.toDouble()
        else when (tagValue) {
            "RU:rural" -> 90.0
            "RU:urban" -> 110.0
            else -> 60.0
        }
    }
}