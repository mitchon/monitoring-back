package com.komarov.osmgraphapp.services

import com.komarov.osmgraphapp.components.AStarAlgorithm
import com.komarov.osmgraphapp.components.EuclideanDistance
import com.komarov.osmgraphapp.converters.LocationConverter
import com.komarov.osmgraphapp.converters.LocationLinkConverter
import com.komarov.osmgraphapp.converters.VertexConverter
import com.komarov.osmgraphapp.models.LocationLink
import com.komarov.osmgraphapp.repositories.LocationLinkRepository
import com.komarov.osmgraphapp.repositories.LocationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.util.StopWatch

@Service
class ShortestPathService(
    private val locationRepository: LocationRepository,
    private val locationLinkRepository: LocationLinkRepository,
    private val locationConverter: LocationConverter,
    private val locationLinkConverter: LocationLinkConverter,
    private val vertexConverter: VertexConverter,
) {
    private val stopWatch = StopWatch()
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val heuristic = EuclideanDistance()
    private val algorithm = AStarAlgorithm<Long>(heuristic)

    fun getRouteAStarDefault(from: Long, to: Long): List<LocationLink> {
        stopWatch.start("default algorithm")
        val locations = locationRepository.findAll()
        val links = locationLinkRepository.findAll()
        val start = locations.firstOrNull { it.id == from }?.let {
            vertexConverter.convert(it)
        }
        val goal = locations.firstOrNull { it.id == to }?.let {
            vertexConverter.convert(it)
        }
        if (start == null || goal == null)
            throw RuntimeException("BadArgumentException, from or to locations are not found")
        val route = algorithm.getRoute(start, goal) { current ->
            links.filter { it.start.id == current.id }.map {
                (vertexConverter.convert(it.finish) to it.length)
            }
        }
        stopWatch.stop()
        logger.info("default algorithm ${stopWatch.lastTaskInfo.timeMillis}")
        val linksSet = route?.zipWithNext()?.map {
            links.firstOrNull { ll -> ll.start.id == it.first.id && ll.finish.id == it.second.id }!!.let {
                locationLinkConverter.convert(it)
            }
        } ?: throw RuntimeException("Route not found")
        return linksSet
    }

    fun getRouteAStarSafeSpace(from: Long, to: Long): List<LocationLink> {
        stopWatch.start("safe-space algorithm")
        val start = locationRepository.findById(from)?.let {
            vertexConverter.convert(it)
        }
        val goal = locationRepository.findById(to)?.let {
            vertexConverter.convert(it)
        }
        if (start == null || goal == null)
            throw RuntimeException("BadArgumentException, from or to locations are not found")
        val route = algorithm.getRoute(start, goal) { current ->
            locationLinkRepository.findByStartId(current.id).map {
                (vertexConverter.convert(it.finish) to it.length)
            }
        }
        stopWatch.stop()
        logger.info("safe-space algorithm ${stopWatch.lastTaskInfo.timeMillis}")
        val linksSet = route?.zipWithNext()?.map {
            locationLinkRepository.findByStartIdAndFinishId(it.first.id, it.second.id)?.let {
                locationLinkConverter.convert(it)
            } ?: throw RuntimeException("Link between ${it.first.id} and ${it.second.id} not found")
        } ?: throw RuntimeException("Route not found")
        return linksSet
    }
}