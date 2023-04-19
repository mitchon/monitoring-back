package com.komarov.osmgraphapp.repositories

import com.komarov.osmgraphapp.entities.LocationEntity
import com.komarov.osmgraphapp.entities.LocationLinkWithLocationsEntity
import com.komarov.osmgraphapp.entities.LocationLinkInsertableEntity
import com.komarov.osmgraphapp.entities.LocationLinkWithFinishEntity
import com.komarov.osmgraphapp.utils.LocationsUpdateEvent
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlBatch
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.jdbi.v3.sqlobject.statement.UseRowMapper
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*


interface LocationLinkEntityJdbiRepository {
    @RegisterKotlinMapper(LocationLinkInsertableEntity::class)
    @SqlBatch("insert into master.location_links values (:start, :finish, :length, :maxSpeed)")
    fun insertBatch(@BindBean nodes: List<LocationLinkInsertableEntity>)

    @UseRowMapper(LocationLinkWithLocationsMapper::class)
    @SqlQuery("select " +
            "s.id as s_id, s.latitude as s_latitude, s.longitude as s_longitude, " +
            "f.id as f_id, f.latitude as f_latitude, f.longitude as f_longitude, " +
            "ll.length, ll.max_speed " +
            "from location_links ll join locations s on ll.start = s.id join locations f on ll.finish = f.id"
    )
    fun findAll(): List<LocationLinkWithLocationsEntity>

    @UseRowMapper(LocationLinkWithFinishMapper::class)
    @SqlQuery("select " +
        "s.id as s_id, " +
        "f.id as f_id, f.latitude as f_latitude, f.longitude as f_longitude, " +
        "ll.length, ll.max_speed " +
        "from location_links ll join locations s on ll.start = s.id join locations f on ll.finish = f.id " +
        "where s.id = ?"
    )
    fun findByStartId(startId: Long): List<LocationLinkWithFinishEntity>

    @UseRowMapper(LocationLinkWithLocationsMapper::class)
    @SqlQuery("select " +
        "s.id as s_id, s.latitude as s_latitude, s.longitude as s_longitude, " +
        "f.id as f_id, f.latitude as f_latitude, f.longitude as f_longitude, " +
        "ll.length, ll.max_speed " +
        "from location_links ll join locations s on ll.start = s.id join locations f on ll.finish = f.id " +
        "where s.id = ? and f.id = ?"
    )
    fun findByStartIdAndFinishId(startId: Long, finishId: Long): LocationLinkWithLocationsEntity?

    @SqlUpdate("delete from master.location_links where true")
    fun deleteAll()
}

class LocationLinkWithLocationsMapper: RowMapper<LocationLinkWithLocationsEntity> {
    @Throws(SQLException::class)
    override fun map(rs: ResultSet, ctx: StatementContext): LocationLinkWithLocationsEntity {
        return LocationLinkWithLocationsEntity(
            start = LocationEntity(
                id = rs.getLong("s_id"),
                latitude = rs.getDouble("s_latitude"),
                longitude = rs.getDouble("s_longitude")
            ),
            finish = LocationEntity(
                id = rs.getLong("f_id"),
                latitude = rs.getDouble("f_latitude"),
                longitude = rs.getDouble("f_longitude")
            ),
            length = rs.getDouble("length"),
            maxSpeed = rs.getDouble("max_speed")
        )
    }
}

class LocationLinkWithFinishMapper: RowMapper<LocationLinkWithFinishEntity> {
    @Throws(SQLException::class)
    override fun map(rs: ResultSet, ctx: StatementContext): LocationLinkWithFinishEntity {
        return LocationLinkWithFinishEntity(
            start = rs.getLong("s_id"),
            finish = LocationEntity(
                id = rs.getLong("f_id"),
                latitude = rs.getDouble("f_latitude"),
                longitude = rs.getDouble("f_longitude")
            ),
            length = rs.getDouble("length"),
            maxSpeed = rs.getDouble("max_speed")
        )
    }
}

@Repository
class LocationLinkRepository(
    private val jdbi: Jdbi,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    private var jdbiRepository = jdbi.onDemand(LocationLinkEntityJdbiRepository::class.java)
    fun findAll() = jdbiRepository.findAll()
    fun insertBatch(links: List<LocationLinkInsertableEntity>) {
        jdbiRepository.insertBatch(links)
        applicationEventPublisher.publishEvent(LocationsUpdateEvent())
    }
    fun findByStartId(startId: Long): List<LocationLinkWithFinishEntity> = jdbiRepository.findByStartId(startId)

    fun findByStartIdAndFinishId(startId: Long, finishId: Long): LocationLinkWithLocationsEntity? =
        jdbiRepository.findByStartIdAndFinishId(startId, finishId)
    fun deleteAll() = jdbiRepository.deleteAll()
}