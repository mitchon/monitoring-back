package com.komarov.osmgraphapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OsmGraphAppApplication

fun main(args: Array<String>) {
    runApplication<OsmGraphAppApplication>(*args)
}
