package com.komarov.osmgraphapp.services

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.komarov.osmgraphapp.components.TextSocketHandler
import de.westnordost.osmapi.map.data.*
import de.westnordost.osmapi.map.handler.MapDataHandler
import de.westnordost.osmapi.overpass.OverpassMapDataApi
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.io.Serializable

val bbox = BoundingBox(35.5,13.8,36.3,14.9)

@Component
class GeomHandler(
    private val objectMapper: ObjectMapper,
    private val textSocketHandler: TextSocketHandler
): MapDataHandler {

    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun handle(bounds: BoundingBox?) {
        logger.info("bounds ${objectMapper.valueToTree<JsonNode>(bounds)}")
    }

    override fun handle(node: Node?) {
        logger.info("node ${objectMapper.valueToTree<JsonNode>(node)}")
    }

    override fun handle(way: Way?) {
        logger.info("way ${objectMapper.valueToTree<JsonNode>(way)}")
        if (way != null)
            textSocketHandler.sendTextMessage(way)
    }

    override fun handle(relation: Relation?) {
        logger.info("relation ${objectMapper.valueToTree<JsonNode>(relation)}")
    }

}

data class LatLng(
    val lat: Double,
    val lng: Double
)

data class LatLngBounds(
    val _northEast: LatLng,
    val _southWest: LatLng
): Serializable

@Service
class GraphService(
    private val overpass: OverpassMapDataApi,
    private val objectMapper: ObjectMapper,
    private val handler: GeomHandler
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun getGraphGeom(bounds: LatLngBounds) {
        val bbox = BoundingBox(
            OsmLatLon(bounds._southWest.lat, bounds._southWest.lng),
            OsmLatLon(bounds._northEast.lat, bounds._northEast.lng)
        )
        overpass.queryElements(
            """
                [bbox:${bbox.minLatitude},${bbox.minLongitude},${bbox.maxLatitude},${bbox.maxLongitude}];
                (
                    way
                    ['highway' !~ 'path']
                    ['highway' !~ 'steps']
                    ['highway' !~ 'track']
                    ['highway' !~ 'escape']
                    ['highway' !~ 'raceway']
                    ['highway' !~ 'busway']
                    ['highway' !~ 'corridor']
                    ['highway' !~ 'via_ferrata']
                    ['highway' !~ 'pedestrian']
                    ['highway' !~ 'bridleway']
                    ['highway' !~ 'proposed']
                    ['highway' !~ 'construction']
                    ['highway' !~ 'elevator']
                    ['highway' !~ 'bus_guideway']
                    ['highway' !~ 'footway']
                    ['highway' !~ 'cycleway']
                    ['foot' !~ 'no']
                    ['access' !~ 'private']
                    ['access' !~ 'no']
                    
                    ['service' !~ 'parking_aisle']
                    ['service' !~ 'alley']
                    ['service' !~ 'slipway'];
                );
                node(w);
                way(bn);
                out meta;
            """.trimIndent(),
            handler
        )
    }
}