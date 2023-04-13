package com.komarov.osmgraphapp.components

import org.springframework.stereotype.Component
import java.util.*
import kotlin.math.*

abstract class GraphAbstractAlgorithm<TEdge, TVertex> {

    abstract fun getRoute (
        start: Vertex<TEdge, TVertex>,
        goal: Vertex<TEdge, TVertex>,
        outgoingEdges: (Vertex<TEdge, TVertex>) -> List<Edge<TEdge, TVertex>>
    ): List<Edge<TEdge, TVertex>>?
}

class Vertex<TEdge, TVertex> (
    val backing: TVertex,
    val lat: Double,
    val lon: Double,
    var g: Double = Double.MAX_VALUE,
    var h: Double = 0.0,
    var parentEdge: Edge<TEdge, TVertex>? = null
) {
    val f: Double
        get() = g + h
}

class Edge<TEdge, TVertex> (
    val backing: TEdge,
    val start: Vertex<TEdge, TVertex>,
    val finish: Vertex<TEdge, TVertex>,
    val length: Double
)

sealed interface Heuristic {
    fun getEstimation(a: Vertex<*, *>, b: Vertex<*, *>): Double
}

open class EuclideanDistance: Heuristic {
    override fun getEstimation(a: Vertex<*, *>, b: Vertex<*, *>): Double {
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

class AStarAlgorithm<TEdge, TVertex>(
    private val heuristic: Heuristic
): GraphAbstractAlgorithm<TEdge, TVertex>() {

    override fun getRoute(
        start: Vertex<TEdge, TVertex>,
        goal: Vertex<TEdge, TVertex>,
        outgoingEdges: (Vertex<TEdge, TVertex>) -> List<Edge<TEdge, TVertex>>
    ): List<Edge<TEdge, TVertex>>? {
        val openList = PriorityQueue<Vertex<TEdge, TVertex>>(compareBy { it.f })
        val closedSet = mutableSetOf<Vertex<TEdge, TVertex>>()

        start.g = 0.0
        start.h = heuristic.getEstimation(start, goal)
        openList.add(start)


        while (openList.isNotEmpty()) {
            val current = openList.poll()

            if (current == goal)
                return buildPath(current)

            closedSet.add(current)

            for (edge in outgoingEdges(current)) {
                val neighbor = edge.finish
                val score = current.g + edge.length

                if (neighbor in closedSet && score >= neighbor.g)
                    continue

                neighbor.parentEdge = edge
                neighbor.g = score
                neighbor.h = heuristic.getEstimation(neighbor, goal)

                if (neighbor !in openList) {
                    openList.add(neighbor)
                }
            }
        }

        return null
    }

    private fun buildPath(current: Vertex<TEdge, TVertex>): List<Edge<TEdge, TVertex>> {
        val path = mutableListOf<Edge<TEdge, TVertex>>()
        var temp: Edge<TEdge, TVertex>? = current.parentEdge
        while (temp != null) {
            path.add(0, temp)
            temp = temp.start.parentEdge
        }
        return path
    }
}