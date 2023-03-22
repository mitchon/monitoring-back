package com.komarov.osmgraphapp.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.komarov.osmgraphapp.entities.NodeEntity
import com.komarov.osmgraphapp.entities.WayEntity
import com.komarov.osmgraphapp.handlers.NodesHandler
import com.komarov.osmgraphapp.handlers.RoadwaysHandler
import com.komarov.osmgraphapp.models.BoundingBoxTemplate
import com.komarov.osmgraphapp.models.RequestResponse
import com.komarov.osmgraphapp.repositories.NodeRepository
import com.komarov.osmgraphapp.repositories.WayRepository
import de.westnordost.osmapi.map.data.*
import de.westnordost.osmapi.overpass.OverpassMapDataApi
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import java.util.*

@Service
class RoadwaysGraphService(
    private val overpass: OverpassMapDataApi,
    private val objectMapper: ObjectMapper,
    private val wayRepository: WayRepository,
    private val nodeRepository: NodeRepository
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun requestRoads(): List<Way> {
        if (wayRepository.findAll().isEmpty()) {
            val boundingBox = BoundingBoxTemplate(
                minLatitude = 54.7335694,
                minLongitude = 38.8318137,
                maxLatitude = 54.7835694,
                maxLongitude = 38.9218137,
            )
            val roadwaysHandler = RoadwaysHandler()
            overpass.queryElements(
                """
                [bbox:${boundingBox.minLatitude},${boundingBox.minLongitude},${boundingBox.maxLatitude},${boundingBox.maxLongitude}];
                (
                    way['highway' = 'motorway'];
                    way['highway' = 'trunk'];
                    way['highway' = 'primary'];
                    way['highway' = 'secondary'];
                    way['highway' = 'tertiary'];
                    way['highway' = 'unclassified'];
                    way['highway' = 'residential'];
                );
                out meta;
            """.trimIndent(),
                roadwaysHandler
            )
            val roadwaysGeom = roadwaysHandler.get().map { way ->
                val nodesHandler = NodesHandler()
                val nodeQueries = way.nodeIds.joinToString(" ") { "node($it);" }
                overpass.queryElements(
                    """
                    ($nodeQueries);
                    out meta;
                """.trimIndent(),
                    nodesHandler
                )
                Way(
                    id = way.id,
                    nodes = nodesHandler.get().map {
                        Node(
                            id = it.id,
                            latitude = it.position.latitude,
                            longitude = it.position.longitude
                        )
                    }
                )
            }.distinctBy { it.id }
            wayRepository.insertBatch(roadwaysGeom.map { WayEntity(it.id) })
            nodeRepository.insertBatch(roadwaysGeom.flatMap { way ->
                way.nodes.map {
                    NodeEntity(
                        id = it.id,
                        wayId = way.id,
                        latitude = it.latitude,
                        longitude = it.longitude
                    )
                }
            })
            return roadwaysGeom
        } else {
            val ways = wayRepository.findAll()
            val roadwaysGeom = ways.map { way ->
                Way(
                    id = way.id,
                    nodes = way.nodes.map {
                        Node(
                            id = it.id,
                            latitude = it.latitude,
                            longitude = it.longitude
                        )
                    }
                )
            }
            return roadwaysGeom
        }
        logger.info("http session ${RequestContextHolder.currentRequestAttributes().sessionId}")
    }
}

data class Node(
    val id: Long,
    val latitude: Double,
    val longitude: Double
)

data class Way(
    val id: Long,
    val nodes: List<Node>
)