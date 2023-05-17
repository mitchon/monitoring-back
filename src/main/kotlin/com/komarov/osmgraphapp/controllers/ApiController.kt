package com.komarov.osmgraphapp.controllers

import com.komarov.osmgraphapp.models.LocationLink
import com.komarov.osmgraphapp.models.RequestResponse
import com.komarov.osmgraphapp.services.RoadwaysGraphService
import com.komarov.osmgraphapp.services.ShortestPathService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/graph")
class ApiController(
    private val roadwaysGraphService: RoadwaysGraphService,
    private val shortestPathService: ShortestPathService
) {
    @GetMapping("/build")
    fun buildGraph(): RequestResponse {
        return roadwaysGraphService.requestBuild()
    }

    @GetMapping("/graph")
    fun getWays(): List<LocationLink> {
        return roadwaysGraphService.requestGraph()
    }

    @GetMapping("/route/{start}/{finish}/a-star/default")
    fun getRouteAStarDefault(@PathVariable start: Long, @PathVariable finish: Long): List<LocationLink>? {
        return shortestPathService.getRouteAStarDefault(start, finish)
    }

    @GetMapping("/route/{start}/{finish}/a-star/safe-space-cached-time")
    fun getRouteAStarSafeSpace(@PathVariable start: Long, @PathVariable finish: Long): List<LocationLink>? {
        return shortestPathService.getRouteAStarSafeSpaceCachedTimeHeuristic(start, finish, 5000)
    }

    @GetMapping("/route/{start}/{finish}/a-star/safe-space-cached")
    fun getRouteAStarSafeSpaceCached(@PathVariable start: Long, @PathVariable finish: Long): List<LocationLink>? {
        return shortestPathService.getRouteAStarSafeSpaceCached(start, finish, 5000)
    }

    @GetMapping("/route/{start}/{finish}/a-star/parallel")
    fun getRouteAStarParallel(@PathVariable start: Long, @PathVariable finish: Long): List<LocationLink>? {
        return shortestPathService.getRouteAStarParallel(start, finish)
    }
}