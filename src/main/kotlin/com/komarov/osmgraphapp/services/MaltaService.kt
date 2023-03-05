package com.komarov.osmgraphapp.services

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.westnordost.osmapi.map.data.BoundingBox
import de.westnordost.osmapi.map.data.Node
import de.westnordost.osmapi.map.data.Relation
import de.westnordost.osmapi.map.data.Way
import de.westnordost.osmapi.map.handler.MapDataHandler
import de.westnordost.osmapi.overpass.OverpassMapDataApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

val bbox = BoundingBox(35.5,13.8,36.3,14.9)

class MaltaHandler(
    private val logger: Logger,
    private val objectMapper: ObjectMapper
): MapDataHandler {
    override fun handle(bounds: BoundingBox?) {
        logger.info("bounds ${objectMapper.valueToTree<JsonNode>(bounds)}")
        if (bounds?.maxLatitude!! > bbox.maxLatitude || bounds?.minLatitude!! < bbox.minLatitude)
            throw Exception()
    }

    override fun handle(node: Node?) {
        logger.info("node ${objectMapper.valueToTree<JsonNode>(node)}")
    }

    override fun handle(way: Way?) {
        logger.info("way ${objectMapper.valueToTree<JsonNode>(way)}")
    }

    override fun handle(relation: Relation?) {
        logger.info("relation ${objectMapper.valueToTree<JsonNode>(relation)}")
    }

}

@Service
class MaltaService(
    private val overpass: OverpassMapDataApi,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    fun getMaltaShops() {
        val handler = MaltaHandler(logger, objectMapper)
        overpass.queryElements(
            """node["shop"](${bbox.minLatitude},${bbox.minLongitude},${bbox.maxLatitude},${bbox.maxLongitude}); out meta;""",
            handler
        )
        val count = overpass.queryCount(
            """node["shop"](${bbox.minLatitude},${bbox.minLongitude},${bbox.maxLatitude},${bbox.maxLongitude}); out count;"""
        )
        logger.info("count ${objectMapper.valueToTree<JsonNode>(count)}")
    }
}