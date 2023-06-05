package com.komarov.osmgraphapp.repositories

import com.komarov.osmgraphapp.entities.BorderEntity
import com.komarov.osmgraphapp.entities.BorderInsertableEntity
import com.komarov.osmgraphapp.entities.LocationEntity
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlBatch
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.UseRowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

interface BorderJdbiRepository {
    @UseRowMapper(BorderWithLocationMapper::class)
    @SqlQuery(
        "select " +
            "l.id as l_id, l.latitude as l_latitude, l.longitude as l_longitude, l.district as l_district, l.type as l_type, " +
            "b.id as b_id, b.from_district, b.to_district " +
            "from master.borders b join master.locations l on b.location_id = l.id "
    )
    fun findAll(): List<BorderEntity>

    @UseRowMapper(BorderWithLocationMapper::class)
    @SqlQuery(
        "with loc as (select * from locations where id = :id) " +
            "select " +
            "l.id as l_id, l.latitude as l_latitude, l.longitude as l_longitude, l.district as l_district, l.type as l_type, " +
            "b.id as b_id, b.from_district, b.to_district " +
            "from master.borders b join master.locations l on b.location_id = l.id " +
            "join loc on 1=1 " +
            "where b.from_district = :from and b.to_district = :to " +
            "order by " +
            "st_distancesphere(geometry(point(l.longitude, l.latitude)), geometry(point(loc.longitude, loc.latitude))) " +
            "limit 1"
    )
    fun findClosest(@Bind from: String, @Bind to: String, @Bind id: Long): BorderEntity

    @RegisterKotlinMapper(BorderInsertableEntity::class)
    @SqlBatch("insert into master.borders values (:id, :fromDistrict, :toDistrict, :location)")
    fun insertBatch(@BindBean borders: List<BorderInsertableEntity>)
}

class BorderWithLocationMapper: RowMapper<BorderEntity> {
    @Throws(SQLException::class)
    override fun map(rs: ResultSet, ctx: StatementContext): BorderEntity {
        return BorderEntity(
            id = rs.getObject("b_id", UUID::class.java),
            fromDistrict = rs.getString("from_district"),
            toDistrict = rs.getString("to_district"),
            location = LocationEntity(
                id = rs.getLong("l_id"),
                latitude = rs.getDouble("l_latitude"),
                longitude = rs.getDouble("l_longitude"),
                district = rs.getString("l_district"),
                type = rs.getString("l_type")
            )
        )
    }

}

@Repository
class BorderRepository(
    jdbi: Jdbi
) {
    private val jdbiRepository = jdbi.onDemand(BorderJdbiRepository::class.java)

    fun insertBatch(borders: List<BorderInsertableEntity>) = jdbiRepository.insertBatch(borders)
    fun findAll() = jdbiRepository.findAll()
    fun findClosest(from: String, to: String, id: Long) = jdbiRepository.findClosest(from, to, id)
}