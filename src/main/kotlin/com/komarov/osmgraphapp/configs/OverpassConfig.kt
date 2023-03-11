package com.komarov.osmgraphapp.configs

import de.westnordost.osmapi.OsmConnection
import de.westnordost.osmapi.overpass.OverpassMapDataApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class OverpassConfig {
    @Bean
    fun connect(): OverpassMapDataApi {
        val connection = OsmConnection("https://overpass-api.de/api/", "osm-graph-app")
        return OverpassMapDataApi(connection)
    }
}