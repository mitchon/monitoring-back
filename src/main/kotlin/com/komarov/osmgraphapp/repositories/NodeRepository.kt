package com.komarov.osmgraphapp.repositories

import com.komarov.osmgraphapp.entities.NodeEntity
import com.komarov.osmgraphapp.entities.UserEntity
import com.komarov.osmgraphapp.entities.WayEntity
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.kotlin.BindKotlin
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlBatch
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.springframework.stereotype.Repository
import java.util.*


interface NodeEntityJdbiRepository {
    @RegisterKotlinMapper(NodeEntity::class)
    @SqlBatch("insert into nodes values (:id, :wayId, :latitude, :longitude)")
    fun insertBatch(@BindBean nodes: List<NodeEntity>)
    @RegisterKotlinMapper(NodeEntity::class)
    @SqlQuery("select n.id as node_id, way_id, n.latitude, n.longitude from master.nodes n")
    fun findAll(): List<NodeEntity>
}

@Repository
class NodeRepository(
    private val jdbi: Jdbi,
) {
    private var jdbiRepository = jdbi.onDemand(NodeEntityJdbiRepository::class.java)
    fun findAll() = jdbiRepository.findAll()
    fun insertBatch(nodes: List<NodeEntity>) = jdbiRepository.insertBatch(nodes)
}