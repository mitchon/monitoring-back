package com.komarov.osmgraphapp.repositories

import com.komarov.osmgraphapp.entities.LocationEntity
import com.komarov.osmgraphapp.entities.LocationLinkEntity
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.result.LinkedHashMapRowReducer
import org.jdbi.v3.core.result.RowView
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlBatch
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.sqlobject.statement.UseRowReducer
import org.springframework.stereotype.Repository
import java.util.*


class LocationLinkReducer: LinkedHashMapRowReducer<Long, LocationEntity> {
    override fun accumulate(container: MutableMap<Long, LocationEntity>, rowView: RowView) {
        val location: LocationEntity = container.computeIfAbsent(
            rowView.getColumn("id", Long::class.javaObjectType)
        ) {
            rowView.getRow(LocationEntity::class.java)
        }

        if (rowView.getColumn("start", Long::class.javaObjectType) != null) {
            location.links.add(rowView.getRow(LocationLinkEntity::class.java))
        }
    }

}

interface LocationEntityJdbiRepository {

    @RegisterKotlinMapper(LocationEntity::class)
    @SqlBatch("insert into master.locations values (:id, :latitude, :longitude)")
    fun insertBatch(@BindBean locations: List<LocationEntity>)

    @RegisterKotlinMapper(LocationEntity::class)
    @RegisterKotlinMapper(LocationLinkEntity::class)
    @SqlQuery("select * from master.locations l left join master.location_links ll on l.id = ll.start")
    @UseRowReducer(LocationLinkReducer::class)
    fun findAll(): List<LocationEntity>

    @RegisterKotlinMapper(LocationEntity::class)
    @SqlUpdate("delete from master.locations where true")
    fun deleteAll()
}

@Repository
class LocationRepository(
    jdbi: Jdbi,
) {
    private var jdbiRepository = jdbi.onDemand(LocationEntityJdbiRepository::class.java)
    fun findAll() = jdbiRepository.findAll()
    fun insertBatch(locations: List<LocationEntity>) = jdbiRepository.insertBatch(locations)
    fun deleteAll() = jdbiRepository.deleteAll()
}