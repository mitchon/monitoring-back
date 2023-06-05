package com.komarov.osmgraphapp.services

import com.komarov.osmgraphapp.models.LocationLink
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.util.StopWatch
import kotlin.system.measureTimeMillis

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
        (252931894L to 92338488L) to 1026,
        (335463849L to 8507900648L) to 1590,
        (253951166L to 499953776L) to 2886,
        (8196509364L to 2513308832L) to 4847,
        (3885130156L to 5116889369L) to 6373,
        (1787731466L to 1238792956L) to 8076,
        (667288052L to 10703096931L) to 9422,
        (302112360L to 7707691957L) to 11321,
        (6115944689L to 80241306L) to 13832,
//        (4856083198L to 2107326315L) to 16788,
//        (3933673224L to 1937115347L) to 21590,
//        (68878578L to 10786867520L) to 24291,
    )

    private fun StopWatch.runTaskNTimes (N: Int, task: () -> Any?): Double {
        this.start()
        for (i in 0.until(N)) { task.invoke() }
        this.stop()
        return this.lastTaskTimeMillis.toDouble() / N
    }

    private fun getTimeAndNumberOfLinks(list: List<LocationLink>): Pair<Double, > {
        return list.sumOf { it.length / (it.maxSpeed / 3.6) }
    }

    @Test
    fun test() {
        listOfInputs.forEach { (coords, dist) ->
            val (from, to) = coords
            logger.info("distance $dist")
            val stopWatch = StopWatch()
            stopWatch.runTaskNTimes(1) { service.default(from, to) }
                .let { logger.info("default    ${it.toString().replace(".", ",")}") }
            stopWatch.runTaskNTimes(1) { service.safeSpace(from, to) }
                .let { logger.info("safe-space ${it.toString().replace(".", ",")}") }
            stopWatch.runTaskNTimes(1) { service.cached(from, to, 7500) }
                .let { logger.info("cached     ${it.toString().replace(".", ",")}") }
            logger.info("time ${getTime(service.cached(from, to, 7500)!!)}")
            stopWatch.runTaskNTimes(1) { service.parallelByDistrict(from, to) }
                .let { logger.info("parallel   ${it.toString().replace(".", ",")}") }
            logger.info("time ${getTime(service.parallelByDistrict(from, to)!!)}")
        }
    }

    @Test
    fun testExtensive() {

    }
}