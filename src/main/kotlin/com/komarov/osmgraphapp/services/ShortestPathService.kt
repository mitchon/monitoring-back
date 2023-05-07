package com.komarov.osmgraphapp.services

import com.komarov.osmgraphapp.components.AStarAlgorithm
import com.komarov.osmgraphapp.components.DistanceHeuristic
import com.komarov.osmgraphapp.components.TimeHeuristic
import com.komarov.osmgraphapp.converters.LocationLinkConverter
import com.komarov.osmgraphapp.converters.VertexConverter
import com.komarov.osmgraphapp.entities.LocationLinkWithFinishAndStatusEntity
import com.komarov.osmgraphapp.models.LocationLink
import com.komarov.osmgraphapp.repositories.LocationLinkRepository
import com.komarov.osmgraphapp.repositories.LocationRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ShortestPathService(
    private val locationRepository: LocationRepository,
    private val locationLinkRepository: LocationLinkRepository,
    private val locationLinkConverter: LocationLinkConverter,
    private val vertexConverter: VertexConverter,
) {
    private val distanceHeuristic = DistanceHeuristic()
    private val timeHeuristic = TimeHeuristic()
    private val algorithm = AStarAlgorithm<Long>(distanceHeuristic)
    private val algorithmWithTime = AStarAlgorithm<Long>(timeHeuristic)

    fun getRouteAStarDefault(from: Long, to: Long): List<LocationLink> {
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
        val linksSet = route?.zipWithNext()?.map {
            links.firstOrNull { ll -> ll.start.id == it.first.id && ll.finish.id == it.second.id }!!.let {
                locationLinkConverter.convert(it)
            }
        } ?: throw RuntimeException("Route not found")
        return linksSet
    }

    fun getRouteAStarSafeSpace(from: Long, to: Long): List<LocationLink> {
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
        val segments = route?.map { it.id }?.zipWithNext() ?: throw RuntimeException("Route Not Found")
        val linksSet = locationLinkRepository.findByStartIdAndFinishIdIn(segments).map {
            locationLinkConverter.convert(it)
        }
        return linksSet
    }

    fun getRouteAStarSafeSpaceCached(from: Long, to: Long, cacheRadius: Int): List<LocationLink> {
        val cachedNeighbors = mutableMapOf<Long, List<LocationLinkWithFinishAndStatusEntity>>()
        val start = locationRepository.findById(from)?.let {
            vertexConverter.convert(it)
        }
        val goal = locationRepository.findById(to)?.let {
            vertexConverter.convert(it)
        }
        if (start == null || goal == null)
            throw RuntimeException("BadArgumentException, from or to locations are not found")
        cachedNeighbors.putAll(locationLinkRepository.findInRadiusAroundId(start.id, cacheRadius).groupBy { it.start })
        val route = algorithm.getRoute(start, goal) { current ->
            val neighboringLinks = cachedNeighbors[current.id] ?: listOf()
            if (neighboringLinks.any { it.needsReload })
                cachedNeighbors.putAll(locationLinkRepository.findInRadiusAroundId(current.id, cacheRadius).groupBy { it.start })
            neighboringLinks.map {
                vertexConverter.convert(it.finish) to it.length
            }
        }
        val segments = route?.map { it.id }?.zipWithNext() ?: throw RuntimeException("Route Not Found")
        val linksSet = locationLinkRepository.findByStartIdAndFinishIdIn(segments).map {
            locationLinkConverter.convert(it)
        }
        return linksSet
    }

    fun getRouteAStarSafeSpaceCachedTimeHeuristic(from: Long, to: Long, cacheRadius: Int): List<LocationLink> {
        val cachedNeighbors = mutableMapOf<Long, List<LocationLinkWithFinishAndStatusEntity>>()
        val start = locationRepository.findById(from)?.let {
            vertexConverter.convert(it)
        }
        val goal = locationRepository.findById(to)?.let {
            vertexConverter.convert(it)
        }
        if (start == null || goal == null)
            throw RuntimeException("BadArgumentException, from or to locations are not found")
        cachedNeighbors.putAll(locationLinkRepository.findInRadiusAroundId(start.id ,cacheRadius).groupBy { it.start })
        val route = algorithmWithTime.getRoute(start, goal) { current ->
            val neighboringLinks = cachedNeighbors[current.id] ?: listOf()
            if (neighboringLinks.any { it.needsReload })
                cachedNeighbors.putAll(locationLinkRepository.findInRadiusAroundId(current.id, cacheRadius).groupBy { it.start })
            neighboringLinks.map {
                vertexConverter.convert(it.finish) to (it.length / ( it.maxSpeed / 3.6 ))
            }
        }
        val segments = route?.map { it.id }?.zipWithNext() ?: throw RuntimeException("Route Not Found")
        val linksSet = locationLinkRepository.findByStartIdAndFinishIdIn(segments).map {
            locationLinkConverter.convert(it)
        }
        return linksSet
    }
}