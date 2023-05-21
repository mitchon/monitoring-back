package com.komarov.osmgraphapp.services

import com.komarov.osmgraphapp.components.*
import com.komarov.osmgraphapp.converters.LocationLinkConverter
import com.komarov.osmgraphapp.converters.VertexConverter
import com.komarov.osmgraphapp.entities.LocationEntity
import com.komarov.osmgraphapp.entities.LocationLinkWithFinishAndStatusEntity
import com.komarov.osmgraphapp.models.LocationLink
import com.komarov.osmgraphapp.repositories.LocationLinkRepository
import com.komarov.osmgraphapp.repositories.LocationRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.jgrapht.alg.shortestpath.DijkstraShortestPath
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.DirectedMultigraph
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

typealias CachedType = MutableMap<Long, List<LocationLinkWithFinishAndStatusEntity>>

@Service
class ShortestPathService(
    private val locationRepository: LocationRepository,
    private val locationLinkRepository: LocationLinkRepository,
    private val locationLinkConverter: LocationLinkConverter,
    private val vertexConverter: VertexConverter,
) {
    private val distanceHeuristic = DistanceHeuristic()
    private val distanceHeuristicParallel = DistanceHeuristicParallel()
    private val timeHeuristic = TimeHeuristic()
    private val algorithm = AStarAlgorithm<Long>(distanceHeuristic)
    private val algorithmParallel = AStarAlgorithmParallel<Long>(distanceHeuristicParallel)
    private val algorithmWithTime = AStarAlgorithm<Long>(timeHeuristic)
    private val logger = LoggerFactory.getLogger(this::class.java)

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
        val cachedNeighbors: CachedType = mutableMapOf()
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
        val cachedNeighbors: CachedType = mutableMapOf()
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

    suspend fun parallel(from: LocationEntity, to: LocationEntity, cacheRadius: Int, cachedNeighbors: CachedType): List<Pair<Long, Long>>? {
        val start = from.let { vertexConverter.convert(it) }
        val goal = to.let { vertexConverter.convert(it) }

        val route = algorithmParallel.getRoute(start, goal) { current ->
            val neighboringLinks = cachedNeighbors[current.id] ?: listOf()
            if (neighboringLinks.any { it.needsReload })
                cachedNeighbors.putAll(locationLinkRepository.findInRadiusAroundId(current.id, cacheRadius).groupBy { it.start })
            neighboringLinks.map {
                vertexConverter.convert(it.finish) to it.length
            }
        }
        return route?.map { it.id }?.zipWithNext() ?: return null
    }

    fun parallelComplete(from: Long, to: Long, cacheRadius: Int): List<LocationLink>? = runBlocking {
        val globalStart = locationRepository.findById(from)
        val globalGoal = locationRepository.findById(to)
        if (globalStart == null || globalGoal == null)
            throw RuntimeException("BadArgumentException, from or to locations are not found")
        val cachedNeighbors: CachedType = mutableMapOf()
        cachedNeighbors.putAll(locationLinkRepository.findInRadiusAroundId(globalStart.id, cacheRadius).groupBy { it.start })

        val startDistrict = globalStart.district
        val goalDistrict = globalGoal.district
        val listOfDistricts: List<String> =
            dijkstraForBorders.getPath(startDistrict, goalDistrict).vertexList
        if (listOfDistricts.size == 1)
            return@runBlocking parallel(globalStart, globalGoal, cacheRadius, cachedNeighbors)?.let {
                convertRoute(it)
            }
        val listOfTransitions = listOfDistricts.zipWithNext()
        val middle = listOfTransitions.size / 2
        val fw = listOfTransitions.slice(0 until middle)
        val rw = listOfTransitions.reversed().slice(0 until middle)

        val firstHalf = async {
            processFw(globalStart, fw, cacheRadius, cachedNeighbors, 0)?.let {
                if (it.isNotEmpty())
                    convertRoute(it)
                else listOf()
            }
        }
        val secondHalf = async {
            processRw(globalGoal, rw, cacheRadius, cachedNeighbors, 0)?.let {
                if (it.isNotEmpty())
                    convertRoute(it)
                else listOf()
            }
        }

        val way = (
            (firstHalf.await() ?: return@runBlocking null) to
            (secondHalf.await() ?: return@runBlocking null)
        )

        val middleStart = way.first.lastOrNull()?.finish?.id?.let {
            locationRepository.findById(it)
        } ?: globalStart
        val middleFinish = way.second.firstOrNull()?.start?.id?.let {
            locationRepository.findById(it)
        } ?: globalGoal

        val middlePart = parallel(middleStart, middleFinish, cacheRadius, cachedNeighbors)?.let {
            convertRoute(it)
        } ?: return@runBlocking null

        return@runBlocking way.first + middlePart + way.second
    }

    private suspend fun convertRoute(route: List<Pair<Long, Long>>): List<LocationLink> {
        return locationLinkRepository.findByStartIdAndFinishIdIn(route).map {
            locationLinkConverter.convert(it)
        }
    }

    private suspend fun processFw(
        start: LocationEntity,
        segments: List<Pair<String, String>>,
        cacheRadius: Int,
        cachedNeighbors: CachedType,
        iteration: Int
    ): List<Pair<Long, Long>>? {
        if (segments.isEmpty()) return listOf()
        val end = locationRepository.findClosestBorder(segments[iteration].first, segments[iteration].second, start.id).location
        val current = parallel(start, end, cacheRadius, cachedNeighbors) ?: return null
        if (iteration >= segments.size - 1)
            return current
        else {
            val next = processFw(end, segments, cacheRadius, cachedNeighbors, iteration + 1) ?: return null
            return current + next
        }
    }

    private suspend fun processRw(
        end: LocationEntity,
        segments: List<Pair<String, String>>,
        cacheRadius: Int,
        cachedNeighbors: CachedType,
        iteration: Int
    ): List<Pair<Long, Long>>? {
        if (segments.isEmpty()) return listOf()
        val start = locationRepository.findClosestBorder(segments[iteration].first, segments[iteration].second, end.id).location
        val current = parallel(start, end, cacheRadius, cachedNeighbors) ?: return null
        if (iteration >= segments.size - 1)
            return current
        else {
            val next = processFw(end, segments, cacheRadius, cachedNeighbors, iteration + 1) ?: return null
            return next + current
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