package com.komarov.osmgraphapp.controllers

import com.komarov.osmgraphapp.models.BoundingBoxTemplate
import com.komarov.osmgraphapp.models.RequestResponse
import com.komarov.osmgraphapp.services.Node
import com.komarov.osmgraphapp.services.RoadwaysGraphService
import com.komarov.osmgraphapp.services.Way
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/graph")
class GraphController(
    private val roadwaysGraphService: RoadwaysGraphService
) {
    @GetMapping("/geom")
    fun getGraphGeom() : List<Way> {
        return roadwaysGraphService.requestRoads()
    }
}