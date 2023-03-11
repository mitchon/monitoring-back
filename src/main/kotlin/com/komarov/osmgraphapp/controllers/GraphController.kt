package com.komarov.osmgraphapp.controllers

import com.komarov.osmgraphapp.services.GraphService
import com.komarov.osmgraphapp.services.LatLngBounds
import de.westnordost.osmapi.map.data.BoundingBox
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/graph")
class GraphController(
    private val graphService: GraphService
) {
    @GetMapping("/geom")
    fun getGraphGeom(@RequestParam bounds: LatLngBounds) {
        graphService.getGraphGeom(bounds)
    }
}