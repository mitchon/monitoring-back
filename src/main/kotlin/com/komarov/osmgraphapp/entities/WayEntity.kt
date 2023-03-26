package com.komarov.osmgraphapp.entities

import org.jdbi.v3.core.mapper.reflect.ColumnName
import java.util.*

data class WayEntity (
    @ColumnName("way_pk")
    val id: UUID = UUID.randomUUID(),
    @ColumnName("osm_way")
    val osmId: Long,
    val nodes: MutableList<NodeEntity> = mutableListOf()
)

data class NodeEntity (
    @ColumnName("node_pk")
    val id: UUID = UUID.randomUUID(),
    @ColumnName("osm_node")
    val osmId: Long,
    @ColumnName("way_fk")
    val wayId: Long,
    val latitude: Double,
    val longitude: Double,
    val index: Int
)