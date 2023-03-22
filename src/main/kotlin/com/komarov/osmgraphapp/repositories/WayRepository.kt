package com.komarov.osmgraphapp.repositories

import com.komarov.osmgraphapp.entities.NodeEntity
import com.komarov.osmgraphapp.entities.WayEntity
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinMapper
import org.jdbi.v3.core.mapper.RowMapperFactory
import org.jdbi.v3.core.result.LinkedHashMapRowReducer
import org.jdbi.v3.core.result.RowView
import org.jdbi.v3.sqlobject.config.RegisterJoinRowMapper
import org.jdbi.v3.sqlobject.config.RegisterRowMapperFactory
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlBatch
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.UseRowReducer
import org.springframework.stereotype.Repository
import java.util.*


class WayNodeReducer: LinkedHashMapRowReducer<Long, WayEntity> {
    override fun accumulate(container: MutableMap<Long, WayEntity>, rowView: RowView) {
        val way: WayEntity = container.computeIfAbsent(rowView.getColumn("id", Long::class.javaObjectType)) {
            rowView.getRow(WayEntity::class.java)
        }

        if (rowView.getColumn("node_id", Long::class.javaObjectType) != null) {
            way.nodes.add(rowView.getRow(NodeEntity::class.java))
        }
    }

}

interface WayEntityJdbiRepository {

    @RegisterKotlinMapper(WayEntity::class)
    @SqlBatch("insert into ways values (:id)")
    fun insertBatch(@BindBean ways: List<WayEntity>)

    @RegisterKotlinMapper(WayEntity::class)
    @RegisterKotlinMapper(NodeEntity::class)
    @SqlQuery("select w.*, n.id as node_id, n.way_id, n.latitude, n.longitude from master.ways w left join nodes n on n.way_id = w.id")
    @UseRowReducer(WayNodeReducer::class)
    fun findAll(): List<WayEntity>
}

@Repository
class WayRepository(
    private val jdbi: Jdbi,
) {
    private var jdbiRepository = jdbi.onDemand(WayEntityJdbiRepository::class.java)
    fun findAll() = jdbiRepository.findAll()
    fun insertBatch(ways: List<WayEntity>) = jdbiRepository.insertBatch(ways)
}