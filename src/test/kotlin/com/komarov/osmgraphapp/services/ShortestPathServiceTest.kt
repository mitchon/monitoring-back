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
        val listOfInputs = listOf<Pair<Long, Long>>(
            6278590964L to 1395536006L, //167
            314687931L to 274257902L, //236
            3178051172L to 9837040169L, //401
            248048033L to 3067976320L, //784
            252931894L to 92338488L, //1026
            335463849L to 8507900648L, //1590
            253951166L to 499953776L, //2886
            8196509364L to 2513308832L, //4847
            3885130156L to 5116889369L, //6373
            2106224195L to 3871088028L, //8130
            667288052L to 10703096931L, //9422
            302112360L to 7707691957L, //11321
            7812832302L to 91326566L, //13063
            4569357720 to 3414713505, //14919
        )
        listOfInputs.forEach { (from, to) ->
            service.getRouteAStarDefault(from, to)
            service.getRouteAStarSafeSpace(from, to)
            service.getRouteAStarSafeSpaceCached(from, to)
            service.getRouteAStarSafeSpaceCachedTimeHeuristic(from, to)
        }
    }
}