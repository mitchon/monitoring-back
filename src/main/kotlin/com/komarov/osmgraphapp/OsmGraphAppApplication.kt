package com.komarov.osmgraphapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class OsmGraphAppApplication

fun main(args: Array<String>) {
    runApplication<OsmGraphAppApplication>(*args)
}
