package com.komarov.osmgraphapp.services

import com.komarov.osmgraphapp.components.*
import com.komarov.osmgraphapp.converters.LocationLinkConverter
import com.komarov.osmgraphapp.converters.VertexConverter
import com.komarov.osmgraphapp.entities.LocationEntity
import com.komarov.osmgraphapp.entities.LocationLinkWithFinishAndStatusEntity
import com.komarov.osmgraphapp.entities.LocationLinkWithFinishEntity
import com.komarov.osmgraphapp.models.LocationLink
import com.komarov.osmgraphapp.repositories.LocationLinkRepository
import com.komarov.osmgraphapp.repositories.LocationRepository
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DirectedMultigraph
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.Callable
import java.util.concurrent.Executors

typealias CachedTypeStatused = MutableMap<Long, List<LocationLinkWithFinishAndStatusEntity>>
typealias CachedType = Map<Long, List<LocationLinkWithFinishEntity>>

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

    fun default(from: Long, to: Long): List<LocationLink>? {
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
        }
        return linksSet
    }

    fun safeSpace(from: Long, to: Long): List<LocationLink>? {
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
        val segments = route?.map { it.id }?.zipWithNext() ?: return null
        val linksSet = locationLinkRepository.findByStartIdAndFinishIdIn(segments).map {
            locationLinkConverter.convert(it)
        }
        return linksSet
    }

    fun cached(from: Long, to: Long, cacheRadius: Int): List<LocationLink>? {
        val cachedNeighbors: CachedTypeStatused = mutableMapOf()
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
        val segments = route?.map { it.id }?.zipWithNext() ?: return null
        val linksSet = locationLinkRepository.findByStartIdAndFinishIdIn(segments).map {
            locationLinkConverter.convert(it)
        }
        return linksSet
    }

    fun cachedTimeHeuristic(from: Long, to: Long, cacheRadius: Int): List<LocationLink>? {
        val cachedNeighbors: CachedTypeStatused = mutableMapOf()
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
        val segments = route?.map { it.id }?.zipWithNext() ?: return null
        val linksSet = locationLinkRepository.findByStartIdAndFinishIdIn(segments).map {
            locationLinkConverter.convert(it)
        }
        return linksSet
    }

    fun cachedByDistrict(from: LocationEntity, to: LocationEntity, cache: CachedType, districts: List<String>): List<Pair<Long, Long>>? {
        val localCache = mutableMapOf<Long, List<LocationLinkWithFinishEntity>>()
        val start = vertexConverter.convert(from)
        val goal = vertexConverter.convert(to)
        val route = algorithm.getRoute(start, goal) { current ->
            val neighbors = cache[current.id] ?: localCache[current.id] ?: listOf()
            if (neighbors.any { !districts.contains(it.finish.district) && localCache[it.finish.id] == null }) {
                localCache.putAll(
                    locationLinkRepository.findInRadiusAroundId(current.id, 500).map {
                        LocationLinkWithFinishEntity(
                            start = it.start,
                            finish = it.finish,
                            length = it.length,
                            maxSpeed = it.maxSpeed
                        )
                    }.groupBy { it.start }
                )
            }
            neighbors.map { vertexConverter.convert(it.finish) to it.length }
        }
        return route?.map { it.id }?.zipWithNext()
    }

    fun parallelByDistrict(from: Long, to: Long): List<LocationLink>? {
        val globalStart = locationRepository.findById(from)
        val globalGoal = locationRepository.findById(to)
        if (globalStart == null || globalGoal == null)
            throw RuntimeException("BadArgumentException, from or to locations are not found")

        val listOfDistricts: List<String> =
            dijkstraForBorders.getPath(globalStart.district, globalGoal.district).vertexList
        val cache = locationLinkRepository.findInDistrict(listOfDistricts).groupBy { it.start }
        if (listOfDistricts.size == 1)
            return cachedByDistrict(globalStart, globalGoal, cache, listOfDistricts)?.let { convertRoute(it) }

        val listOfTransitions = listOfDistricts.zipWithNext()
        val middle = listOfTransitions.size / 2
        val firstHalf = listOfTransitions.slice(0 until middle)
        val secondHalf = listOfTransitions.reversed().slice(0 until middle)
        var closestToPoint = globalStart.id
        val middlePointsFirstHalf = firstHalf.map {(fromDistr, toDistr) ->
            locationRepository.findClosestBorder(fromDistr, toDistr, closestToPoint).location.also {
                closestToPoint = it.id
            }
        }
        closestToPoint = globalGoal.id
        val middlePointsSecondHalf = secondHalf.map {(fromDistr, toDistr) ->
            locationRepository.findClosestBorder(fromDistr, toDistr, closestToPoint).location.also {
                closestToPoint = it.id
            }
        }.reversed()
        val routeSections = (listOf(globalStart) + middlePointsFirstHalf + middlePointsSecondHalf + listOf(globalGoal)).zipWithNext()
        val executor = Executors.newFixedThreadPool(routeSections.size)
        val workers = routeSections.map {
            Callable<List<Pair<Long, Long>>?> {
                cachedByDistrict(it.first, it.second, cache, listOfDistricts)
            }
        }
        executor.invokeAll(workers)
        return workers.flatMap {
            it.call() ?: listOf()
        }.let { convertRoute(it) }
    }

    private fun convertRoute(route: List<Pair<Long, Long>>): List<LocationLink> {
        return locationLinkRepository.findByStartIdAndFinishIdIn(route).map {
            locationLinkConverter.convert(it)
        }
    }

    private lateinit var dijkstraForBorders: DijkstraShortestPath<String, DefaultEdge>

    init {
        val borders = locationRepository.findBorders()
        val bordersDistinct = borders.map { it.fromDistrict to it.toDistrict }.distinct()
        val districts = listOf("ЦАО","ЮВАО","ВАО","СВАО","САО","СЗАО","ЗАО","ЮЗАО","ЮАО")
        val graph = DirectedMultigraph<String, DefaultEdge>(DefaultEdge::class.java)
        districts.forEach {
            graph.addVertex(it)
        }
        bordersDistinct.forEach {
            graph.addEdge(it.first, it.second)
        }
        dijkstraForBorders = DijkstraShortestPath(graph)
    }
}