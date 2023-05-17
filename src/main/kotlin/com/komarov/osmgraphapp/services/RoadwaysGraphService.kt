package com.komarov.osmgraphapp.services

import com.komarov.osmgraphapp.converters.LocationConverter
import com.komarov.osmgraphapp.converters.LocationLinkConverter
import com.komarov.osmgraphapp.entities.BorderInsertableEntity
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
        logger.info("Got all roads (${roads.size}) by request $requestId")
        logger.info(roads.map { it.maxSpeed }.distinct().joinToString(","))
        val locationsWithLinks = roads.chunked(100)
            .mapIndexed { i ,road ->
                requestLocations(road).also { logger.info("${i}00/${roads.size}") }
            }
            .let { it.flatMap { it.first } to it.flatMap { it.second } }
        val locations = locationsWithLinks.first.distinctBy { it.id }
        val links = locationsWithLinks.second.distinctBy { (it.start.id to it.finish.id) }
        logger.info("Inserting by request $requestId")
        val locationEntities = locations.map {
            LocationEntity.fromModel(it)
        }
        locationRepository.insertBatch(locationEntities)
        val locationLinkEntities = links.map {
            LocationLinkInsertableEntity.fromModel(it)
        }
        locationLinkRepository.insertBatch(locationLinkEntities)
        val borderLocations = findBorderLocations().flatMap { (k, v) ->
            v.map {
                BorderInsertableEntity(
                    id = UUID.randomUUID(),
                    fromDistrict = k.first,
                    toDistrict = k.second,
                    location = it
                )
            }
        }
        locationRepository.insertBordersBatch(borderLocations)
        logger.info("Request $requestId is fulfilled")
        return RequestResponse(requestId)
    }

    private fun findBorderLocations(): Map<Pair<String, String>, List<Long>> {
        val borderLinks = locationLinkRepository.findBorders()
        return borderLinks
            .map { (it.start.district to it.finish.district) to it.start }
            .groupBy { it.first }
            .mapValues { (k, v) -> v.map { it.second.id } }
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

    private fun requestLocations(roads: List<Road>): Pair<List<Location>, List<LocationLink>> {
        val nodesHandler = NodesHandler()
        val nodes: List<Long> = roads.flatMap { it.nodes }.distinct()
        val locations: MutableMap<Long, Location> = mutableMapOf()
        val roadsAssociation = roads.flatMap { road -> road.nodes.map { it to road } }.associate { it.first to it.second }
        overpass.queryElements(
            """
                (
                    ${nodes.joinToString("\n") { "node($it);" }}
                );
                out meta;
            """.trimIndent(),
            nodesHandler
        )
        nodesHandler.get().map { node ->
            val location = Location(
                id = node.id,
                latitude = node.position.latitude,
                longitude = node.position.longitude,
                district = roadsAssociation[node.id]?.district!!,
                type = roadsAssociation[node.id]?.type!!
            )
            locations[location.id] = location
        }
        if (locations.isEmpty()) return (listOf<Location>() to listOf<LocationLink>())
        val listOfResults = roads.mapNotNull { road ->
            val localLocations = road.nodes.mapNotNull { locations[it] }
            if (localLocations.isEmpty()) return@mapNotNull null
            val primaryDirLinks = localLocations.zipWithNext().map { link ->
                val length = countDistance(link.first, link.second)
                LocationLink(
                    start = link.first,
                    finish = link.second,
                    length = length,
                    maxSpeed = countSpeed(road.maxSpeed)
                )
            }

            var secondaryDirLinks = listOf<LocationLink>()

            if (!road.oneway) {
                secondaryDirLinks = primaryDirLinks.map { link ->
                    LocationLink(
                        start = link.finish,
                        finish = link.start,
                        length = link.length,
                        maxSpeed = link.maxSpeed
                    )
                }
            }

            (localLocations to primaryDirLinks + secondaryDirLinks)
        }
        return listOfResults.flatMap { it.first } to listOfResults.flatMap { it.second }
    }

    private fun requestRoads(): List<Road> {
        val waysHandler = WaysHandler()
        val request = """
            (
                way(area.a)['highway' = 'primary'];
                way(area.a)['highway' = 'secondary'];
                way(area.a)['highway' = 'tertiary'];
                way(area.a)['highway' = 'residential'];
                way(area.a)['highway' = 'living_street'];
                way(area.a)['highway' = 'unclassified'];
            );
            out meta;
        """.trimIndent()
        val listOfDistricts = listOf("ЦАО","ЮВАО","ВАО","СВАО","САО","СЗАО","ЗАО","ЮЗАО","ЮАО")
        val allWays = listOfDistricts.flatMap { district ->
            overpass.queryElements(
                "area[ref~\"$district\"]->.a;\n$request",
                waysHandler
            )
            waysHandler.get().map { way -> way to district }
        }
        return allWays.map { (way, district) ->
            val oneway = way.tags["oneway"]?.let { it == "yes" } ?: false
            val roundabout = way.tags["junction"]?.let { it == "roundabout" } ?: false
            val maxSpeed = way.tags["maxspeed"]
            Road(
                id = way.id,
                nodes = way.nodeIds,
                oneway = oneway || roundabout,
                maxSpeed = maxSpeed,
                district = district,
                type = way.tags["highway"] ?: "unclassified"
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
            "RU:urban" -> 60.0
            "RU:living_street" -> 20.0
            else -> 60.0
        }
    }
}