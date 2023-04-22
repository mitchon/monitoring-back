package com.komarov.osmgraphapp.repositories

import com.komarov.osmgraphapp.entities.*
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
        "start as s_id, " +
        "f.id as f_id, f.latitude as f_latitude, f.longitude as f_longitude, " +
        "ll.length, ll.max_speed " +
        "from location_links ll join locations f on ll.finish = f.id " +
        "where start = ?"
    )
    fun findByStartId(startId: Long): List<LocationLinkWithFinishEntity>

    @UseRowMapper(LocationLinkWithFinishAndStatusMapper::class)
    @SqlQuery("with center as (select * from master.locations where id = ?) " +
        "select " +
        "st_distancesphere(" +
        "geometry(point(f.longitude, f.latitude)), geometry(point(center.longitude, center.latitude))" +
        ") > 5000 as needs_reload, " +
        "s.id as s_id, " +
        "f.id as f_id, f.latitude as f_latitude, f.longitude as f_longitude, " +
        "ll.length, ll.max_speed " +
        "from location_links ll join locations s on ll.start = s.id join locations f on ll.finish = f.id " +
        "join center on 1=1 " +
        "where st_distancesphere(" +
        "geometry(point(s.longitude, s.latitude)), geometry(point(center.longitude, center.latitude))" +
        ") <= 5000")
    fun findInRadiusAroundId(id: Long): List<LocationLinkWithFinishAndStatusEntity>

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

class LocationLinkWithFinishAndStatusMapper: RowMapper<LocationLinkWithFinishAndStatusEntity> {
    @Throws(SQLException::class)
    override fun map(rs: ResultSet, ctx: StatementContext): LocationLinkWithFinishAndStatusEntity {
        return LocationLinkWithFinishAndStatusEntity(
            start = rs.getLong("s_id"),
            finish = LocationEntity(
                id = rs.getLong("f_id"),
                latitude = rs.getDouble("f_latitude"),
                longitude = rs.getDouble("f_longitude")
            ),
            length = rs.getDouble("length"),
            maxSpeed = rs.getDouble("max_speed"),
            needsReload = rs.getBoolean("needs_reload")
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

    fun findByStartId(startId: Long): List<LocationLinkWithFinishEntity> {
        return jdbiRepository.findByStartId(startId)
    }

    fun findInRadiusAroundId(id: Long): List<LocationLinkWithFinishAndStatusEntity> {
        return jdbiRepository.findInRadiusAroundId(id)
    }

    fun findByStartIdAndFinishId(startId: Long, finishId: Long): LocationLinkWithLocationsEntity? =
        jdbiRepository.findByStartIdAndFinishId(startId, finishId)
    fun deleteAll() = jdbiRepository.deleteAll()
}