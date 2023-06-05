package com.komarov.osmgraphapp.repositories

import com.komarov.osmgraphapp.entities.*
import com.komarov.osmgraphapp.utils.LocationsUpdateEvent
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.customizer.BindBeanList
import org.jdbi.v3.sqlobject.customizer.BindList
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
            "s.id as s_id, s.latitude as s_latitude, s.longitude as s_longitude, s.district as s_district, s.type as s_type, " +
            "f.id as f_id, f.latitude as f_latitude, f.longitude as f_longitude, f.district as f_district, f.type as f_type, " +
            "ll.length, ll.max_speed " +
            "from location_links ll join locations s on ll.start = s.id join locations f on ll.finish = f.id"
    )
    fun findAll(): List<LocationLinkWithLocationsEntity>

    @UseRowMapper(LocationLinkWithLocationsMapper::class)
    @SqlQuery("select " +
        "s.id as s_id, s.latitude as s_latitude, s.longitude as s_longitude, s.district as s_district, s.type as s_type, " +
        "f.id as f_id, f.latitude as f_latitude, f.longitude as f_longitude, f.district as f_district, f.type as f_type, " +
        "ll.length, ll.max_speed " +
        "from location_links ll join locations s on ll.start = s.id join locations f on ll.finish = f.id " +
        "where s.district != f.district"
    )
    fun findBorderingLinks(): List<LocationLinkWithLocationsEntity>


    @UseRowMapper(LocationLinkWithFinishMapper::class)
    @SqlQuery("select " +
        "start as s_id, " +
        "f.id as f_id, f.latitude as f_latitude, f.longitude as f_longitude, f.district as f_district, f.type as f_type, " +
        "ll.length, ll.max_speed " +
        "from location_links ll join locations f on ll.finish = f.id " +
        "where start = ?"
    )
    fun findByStartId(startId: Long): List<LocationLinkWithFinishEntity>

    @UseRowMapper(LocationLinkWithFinishAndStatusMapper::class)
    @SqlQuery("with center as (select * from master.locations where id = :id) " +
        "select " +
        "st_distancesphere(" +
        "geometry(point(f.longitude, f.latitude)), geometry(point(center.longitude, center.latitude))" +
        ") > :radius as needs_reload, " +
        "s.id as s_id, " +
        "f.id as f_id, f.latitude as f_latitude, f.longitude as f_longitude, f.district as f_district, f.type as f_type, " +
        "ll.length, ll.max_speed " +
        "from location_links ll join locations s on ll.start = s.id join locations f on ll.finish = f.id " +
        "join center on 1=1 " +
        "where st_distancesphere(" +
        "geometry(point(s.longitude, s.latitude)), geometry(point(center.longitude, center.latitude))" +
        ") <= :radius")
    fun findInRadiusAroundId(@Bind("id") id: Long, @Bind("radius") radius: Int): List<LocationLinkWithFinishAndStatusEntity>

    @UseRowMapper(LocationLinkWithFinishMapper::class)
    @SqlQuery("select " +
        "s.id as s_id, " +
        "f.id as f_id, f.latitude as f_latitude, f.longitude as f_longitude, f.district as f_district, f.type as f_type, " +
        "ll.length, ll.max_speed " +
        "from location_links ll join locations s on ll.start = s.id join locations f on ll.finish = f.id " +
        "where s.district in (<districts>) or f.district in (<districts>)")
    fun findInDistrict(@BindList("districts") districts: List<String>): List<LocationLinkWithFinishEntity>

    @UseRowMapper(LocationLinkWithLocationsMapper::class)
    @SqlQuery("select " +
        "s.id as s_id, s.latitude as s_latitude, s.longitude as s_longitude, s.district as s_district, s.type as s_type, " +
        "f.id as f_id, f.latitude as f_latitude, f.longitude as f_longitude, f.district as f_district, f.type as f_type, " +
        "ll.length, ll.max_speed " +
        "from location_links ll join locations s on ll.start = s.id join locations f on ll.finish = f.id " +
        "where (s.id, f.id) in (<segments>)"
    )
    fun findByStartIdAndFinishIdIn(
        @BindBeanList(value = "segments", propertyNames = ["first", "second"]) segments: List<Pair<Long, Long>>
    ): List<LocationLinkWithLocationsEntity>

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
                longitude = rs.getDouble("s_longitude"),
                district = rs.getString("s_district"),
                type = rs.getString("s_type")
            ),
            finish = LocationEntity(
                id = rs.getLong("f_id"),
                latitude = rs.getDouble("f_latitude"),
                longitude = rs.getDouble("f_longitude"),
                district = rs.getString("f_district"),
                type = rs.getString("f_type")
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
                longitude = rs.getDouble("f_longitude"),
                district = rs.getString("f_district"),
                type = rs.getString("f_type")
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
                longitude = rs.getDouble("f_longitude"),
                district = rs.getString("f_district"),
                type = rs.getString("f_type")
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

    fun findInRadiusAroundId(id: Long, radius: Int): List<LocationLinkWithFinishAndStatusEntity> {
        return jdbiRepository.findInRadiusAroundId(id, radius)
    }

    fun findByStartIdAndFinishIdIn(segments: List<Pair<Long, Long>>): List<LocationLinkWithLocationsEntity> =
        jdbiRepository.findByStartIdAndFinishIdIn(segments)

    fun deleteAll() = jdbiRepository.deleteAll()

    fun findBorderingLinks() = jdbiRepository.findBorderingLinks()

    fun findInDistrict(districts: List<String>) = jdbiRepository.findInDistrict(districts)
}