package com.komarov.osmgraphapp.components

import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.coroutineContext
import kotlin.math.*

class Vertex<TVertex> (
    val id: TVertex,
    val lat: Double,
    val lon: Double,
    var g: Double = Double.MAX_VALUE,
    var h: Double = 0.0,
    var parent: Vertex<TVertex>? = null
) {
    val f: Double
        get() = g + h
}

sealed interface Heuristic {
    fun getEstimation(a: Vertex<*>, b: Vertex<*>): Double
}

open class DistanceHeuristic: Heuristic {
    override fun getEstimation(a: Vertex<*>, b: Vertex<*>): Double {
        val earthRadius = 6371000.0 // in meters
        val latDiff = Math.toRadians(b.lat - a.lat)
        val lonDiff = Math.toRadians(b.lon - a.lon)
        val aLat = Math.toRadians(a.lat)
        val bLat = Math.toRadians(b.lat)
        val sinLat = sin(latDiff / 2)
        val sinLon = sin(lonDiff / 2)
        val a1 = sinLat * sinLat + cos(aLat) * cos(bLat) * sinLon * sinLon
        val a2 = 2 * atan2(sqrt(a1), sqrt(1 - a1))
        return earthRadius * a2
    }
}

open class TimeHeuristic: Heuristic {
    override fun getEstimation(a: Vertex<*>, b: Vertex<*>): Double {
        val earthRadius = 6371000.0 // in meters
        val latDiff = Math.toRadians(b.lat - a.lat)
        val lonDiff = Math.toRadians(b.lon - a.lon)
        val aLat = Math.toRadians(a.lat)
        val bLat = Math.toRadians(b.lat)
        val sinLat = sin(latDiff / 2)
        val sinLon = sin(lonDiff / 2)
        val a1 = sinLat * sinLat + cos(aLat) * cos(bLat) * sinLon * sinLon
        val a2 = 2 * atan2(sqrt(a1), sqrt(1 - a1))
        val distance = earthRadius * a2
        return distance / ( 110 / 3.6 )
    }
}

sealed interface HeuristicParallel {
    suspend fun getEstimation(a: Vertex<*>, b: Vertex<*>): Double
}

open class DistanceHeuristicParallel: HeuristicParallel {
    override suspend fun getEstimation(a: Vertex<*>, b: Vertex<*>): Double = runBlocking {
        val earthRadius = 6371000.0 // in meters
        val latDiff = async { Math.toRadians(b.lat - a.lat) }
        val lonDiff = async { Math.toRadians(b.lon - a.lon) }
        val aLat = async { Math.toRadians(a.lat) }
        val bLat = async { Math.toRadians(b.lat) }
        val sinLat = async { sin(latDiff.await() / 2) }
        val sinLon = async { sin(lonDiff.await() / 2) }
        val a1 = sinLat.await() * sinLat.await() + cos(aLat.await()) * cos(bLat.await()) * sinLon.await() * sinLon.await()
        val a2 = 2 * atan2(sqrt(a1), sqrt(1 - a1))
        return@runBlocking earthRadius * a2
    }
}

class AStarAlgorithm<TVertex>(
    private val heuristic: Heuristic
) {
    fun getRoute(
        start: Vertex<TVertex>,
        goal: Vertex<TVertex>,
        neighbors: (Vertex<TVertex>) -> List<Pair<Vertex<TVertex>, Double>>
    ): List<Vertex<TVertex>>? {
        val openList = PriorityQueue<Vertex<TVertex>>(compareBy { it.f })
        val closedSet = mutableMapOf<TVertex, Double>()

        start.g = 0.0
        start.h = heuristic.getEstimation(start, goal)
        openList.add(start)


        while (openList.isNotEmpty()) {
            val current = openList.poll()

            if (current.id == goal.id)
                return buildPath(current)

            closedSet[current.id] = current.g

            for (neighborWithWeight in neighbors(current).filter { it.first.id != current.parent }) {
                val neighbor = neighborWithWeight.first
                val score = current.g + neighborWithWeight.second

                val previousG = closedSet[neighbor.id] ?: neighbor.g
                if (closedSet[neighbor.id] != null && score >= previousG)
                    continue

                neighbor.parent = current
                neighbor.g = score
                neighbor.h = heuristic.getEstimation(neighbor, goal)

                if (!openList.map { it.id }.contains(neighbor.id)) {
                    openList.add(neighbor)
                }
            }
        }

        return null
    }

    private fun buildPath(current: Vertex<TVertex>): List<Vertex<TVertex>> {
        val path = mutableListOf<Vertex<TVertex>>(current)
        var temp: Vertex<TVertex>? = current.parent
        while (temp != null) {
            path.add(0, temp)
            temp = temp.parent
        }
        return path
    }
}

class AStarAlgorithmParallel<TVertex>(
    private val heuristic: HeuristicParallel
) {

    suspend fun getRoute(
        start: Vertex<TVertex>,
        goal: Vertex<TVertex>,
        neighbors: (Vertex<TVertex>) -> List<Pair<Vertex<TVertex>, Double>>
    ): List<Vertex<TVertex>>? = coroutineScope {
        val openList = PriorityQueue<Vertex<TVertex>>(compareBy { it.f })
        val closedSet = mutableMapOf<TVertex, Double>()

        start.g = 0.0
        start.h = heuristic.getEstimation(start, goal)
        openList.add(start)

        suspend fun neighborWorker(
            neighborWithWeight: Pair<Vertex<TVertex>, Double>,
            current: Vertex<TVertex>
        ): Vertex<TVertex>? {
            val neighbor = neighborWithWeight.first
            val score = current.g + neighborWithWeight.second

            val previousG = closedSet[neighbor.id] ?: neighbor.g
            if (closedSet[neighbor.id] != null && score >= previousG)
                return null

            neighbor.parent = current
            neighbor.g = score
            neighbor.h = heuristic.getEstimation(neighbor, goal)

            return if (!openList.map { it.id }.contains(neighbor.id)) {
                return neighbor
            } else null
        }

        while (openList.isNotEmpty()) {
            val current = openList.poll()

            if (current.id == goal.id) {
                return@coroutineScope buildPath(current)
            }

            closedSet[current.id] = current.g

            val neighborsWithWeight = neighbors(current).filter { it.first.id != current.parent }
            neighborsWithWeight.map {
                async { neighborWorker(it, current) }
            }.awaitAll().filterNotNull().forEach {
                openList.add(it)
            }
        }

        return@coroutineScope null
    }

    private fun buildPath(current: Vertex<TVertex>): List<Vertex<TVertex>> {
        val path = mutableListOf<Vertex<TVertex>>(current)
        var temp: Vertex<TVertex>? = current.parent
        while (temp != null) {
            path.add(0, temp)
            temp = temp.parent
        }
        return path
    }
}