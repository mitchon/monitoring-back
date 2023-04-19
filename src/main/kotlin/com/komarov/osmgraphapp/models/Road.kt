package com.komarov.osmgraphapp.models

data class Road(
    val id: Long,
    val nodes: List<Long>,
    val maxSpeed: String?,
    val oneway: Boolean
)