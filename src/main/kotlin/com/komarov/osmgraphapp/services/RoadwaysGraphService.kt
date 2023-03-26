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
                minLatitude = 54.7585694 - 0.07 / 2,
                minLongitude = 38.8818137 - 0.07,
                maxLatitude = 54.7585694 + 0.07 / 2,
                maxLongitude = 38.8818137 + 0.07,
            )
            val roadwaysHandler = RoadwaysHandler()
            overpass.queryElements(
                """
                [bbox:${boundingBox.minLatitude},${boundingBox.minLongitude},${boundingBox.maxLatitude},${boundingBox.maxLongitude}];
                (
                    way['highway' = 'primary'];
                    way['highway' = 'secondary'];
                    way['highway' = 'tertiary'];
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
                    nodes = nodesHandler.get().mapIndexed { i, it ->
                        Node(
                            id = it.id,
                            latitude = it.position.latitude,
                            longitude = it.position.longitude,
                            index = i+1
                        )
                    }
                )
            }
            wayRepository.insertBatch(roadwaysGeom.map {
                WayEntity(
                    osmId = it.id
                )
            })
            nodeRepository.insertBatch(roadwaysGeom.flatMap { way ->
                way.nodes.map {
                    NodeEntity(
                        osmId = it.id,
                        wayId = way.id,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        index = it.index
                    )
                }
            })
            return roadwaysGeom
        } else {
            val ways = wayRepository.findAll()
            val roadwaysGeom = ways.map { way ->
                Way(
                    id = way.osmId,
                    nodes = way.nodes.map {
                        Node(
                            id = it.osmId,
                            latitude = it.latitude,
                            longitude = it.longitude,
                            index = it.index
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
    val longitude: Double,
    val index: Int
)

data class Way(
    val id: Long,
    val nodes: List<Node>
)