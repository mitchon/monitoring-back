package com.komarov.osmgraphapp.controllers

import com.komarov.osmgraphapp.models.LocationLink
import com.komarov.osmgraphapp.models.RequestResponse
import com.komarov.osmgraphapp.services.RoadwaysGraphService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/graph")
class GraphController(
    private val roadwaysGraphService: RoadwaysGraphService
) {
    @GetMapping("/build")
    fun buildGraph(): RequestResponse {
        return roadwaysGraphService.requestBuild()
    }

    @GetMapping("/graph")
    fun getWays(): List<LocationLink> {
        return roadwaysGraphService.requestGraph()
    }
}