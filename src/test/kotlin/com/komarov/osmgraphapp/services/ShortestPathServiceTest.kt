package com.komarov.osmgraphapp.services

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ShortestPathServiceTest(
    @Autowired
    private val service: ShortestPathService
) {
    @Test
    fun test() {
        val from: Long = 8516328888
        val to: Long = 9668012558
        val res1 = service.getRouteAStarDefault(from, to)
        val res2 = service.getRouteAStarSafeSpaceCached(from, to)
        val res3 = service.getRouteAStarSafeSpaceCachedTimeHeuristic(from, to)
    }
}