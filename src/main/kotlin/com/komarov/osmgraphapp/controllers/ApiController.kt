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

    @GetMapping("/route/{start}/{finish}/default")
    fun default(@PathVariable start: Long, @PathVariable finish: Long): List<LocationLink>? {
        return shortestPathService.default(start, finish)
    }

    @GetMapping("/route/{start}/{finish}/safe-space")
    fun safeSpace(@PathVariable start: Long, @PathVariable finish: Long): List<LocationLink>? {
        return shortestPathService.safeSpace(start, finish)
    }

    @GetMapping("/route/{start}/{finish}/cached")
    fun cached(@PathVariable start: Long, @PathVariable finish: Long): List<LocationLink>? {
        return shortestPathService.cached(start, finish, 5000)
    }

    @GetMapping("/route/{start}/{finish}/parallel")
    suspend fun parallel(@PathVariable start: Long, @PathVariable finish: Long): List<LocationLink>? {
        return shortestPathService.parallelByDistrict(start, finish)
    }
}