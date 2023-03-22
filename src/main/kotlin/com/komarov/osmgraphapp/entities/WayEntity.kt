package com.komarov.osmgraphapp.entities

import org.jdbi.v3.core.mapper.reflect.ColumnName

data class WayEntity (
    val id: Long,
    val nodes: MutableList<NodeEntity> = mutableListOf()
)

data class NodeEntity (
    @ColumnName("node_id")
    val id: Long,
    @ColumnName("way_id")
    val wayId: Long,
    val latitude: Double,
    val longitude: Double
)