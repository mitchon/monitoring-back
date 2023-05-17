package com.komarov.osmgraphapp.services

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.util.StopWatch

@SpringBootTest
class ShortestPathServiceTest(
    @Autowired
    private val service: ShortestPathService
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val listOfInputs = listOf<Pair<Pair<Long, Long>, Int>>(
        (6278590964L to 1395536006L) to 167,
        (314687931L to 274257902L) to 236,
        (3178051172L to 9837040169L) to 401,
        (248048033L to 3067976320L) to 784,
        (252931894L to 92338488L) to 1026,
        (335463849L to 8507900648L) to 1590,
        (253951166L to 499953776L) to 2886,
        (8196509364L to 2513308832L) to 4847,
        (3885130156L to 5116889369L) to 6373,
        (2106224195L to 3871088028L) to 8130,
        (667288052L to 10703096931L) to 9422,
        (302112360L to 7707691957L) to 11321,
        (7812832302L to 91326566L) to 13063,
        (4569357720 to 3414713505) to 14919,
    )

    private fun StopWatch.runTaskNTimes (N: Int, task: () -> Any): Double {
        this.start()
        for (i in 0.until(N)) { task.invoke() }
        this.stop()
        return this.lastTaskTimeMillis.toDouble() / N
    }

    @Test
    fun testTime() {
        listOfInputs.forEach { (coords, dist) ->
            val (from, to) = coords
            val time1 = service.getRouteAStarSafeSpaceCached(from, to, 5000)
                .sumOf { it.length / ( it.maxSpeed / 3.6 ) }
            val time2 = service.getRouteAStarSafeSpaceCachedTimeHeuristic(from, to, 5000)
                .sumOf { it.length / ( it.maxSpeed / 3.6 ) }
            logger.info("$dist: $time1, $time2")
            logger.info(if (time2 > time1) "bad" else "good")
        }
    }

    @Test
    fun test() {
        listOfInputs.forEach { (coords, dist) ->
            val (from, to) = coords
            logger.info("distance $dist")
            val stopWatch = StopWatch()
            stopWatch.runTaskNTimes(10) { service.getRouteAStarDefault(from, to) }
                .let { logger.info("default      ${it.toString().replace(".", ",")}") }
//            stopWatch.runTaskNTimes(10) { service.getRouteAStarSafeSpaceCached(from, to, 500) }
//                .let { logger.info("cached 500   ${it.toString().replace(".", ",")}") }
//            stopWatch.runTaskNTimes(10) { service.getRouteAStarSafeSpaceCached(from, to, 1000) }
//                .let { logger.info("cached 1000  ${it.toString().replace(".", ",")}") }
//            stopWatch.runTaskNTimes(10) { service.getRouteAStarSafeSpaceCached(from, to, 2500) }
//            .let { logger.info("cached 2500  ${it.toString().replace(".", ",")}") }
            stopWatch.runTaskNTimes(10) { service.getRouteAStarSafeSpaceCached(from, to, 5000) }
                .let { logger.info("cached 5000  ${it.toString().replace(".", ",")}") }
            stopWatch.runTaskNTimes(10) { service.getRouteAStarSafeSpaceCached(from, to, 7500) }
                .let { logger.info("cached 7500  ${it.toString().replace(".", ",")}") }
            stopWatch.runTaskNTimes(10) { service.getRouteAStarSafeSpaceCached(from, to, 10000) }
                .let { logger.info("cached 10000 ${it.toString().replace(".", ",")}") }
            stopWatch.runTaskNTimes(10) { service.getRouteAStarParallel(from, to) }
                .let { logger.info("split  5000  ${it.toString().replace(".", ",")}") }
        }
    }

    @Test
    fun testExtensive() {

    }
}