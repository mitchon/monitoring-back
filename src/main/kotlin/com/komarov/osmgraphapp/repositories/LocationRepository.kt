package com.komarov.osmgraphapp.repositories

import com.komarov.osmgraphapp.entities.LocationEntity
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlBatch
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.springframework.stereotype.Repository
import java.util.*

interface LocationEntityJdbiRepository {

    @RegisterKotlinMapper(LocationEntity::class)
    @SqlBatch("insert into master.locations values (:id, :latitude, :longitude)")
    fun insertBatch(@BindBean locations: List<LocationEntity>)

    @RegisterKotlinMapper(LocationEntity::class)
    @SqlQuery("select * from master.locations")
    fun findAll(): List<LocationEntity>

    @RegisterKotlinMapper(LocationEntity::class)
    @SqlQuery("select * from master.locations where id = ?")
    fun findById(id: Long): LocationEntity?

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
    fun findById(id: Long) = jdbiRepository.findById(id)
    fun insertBatch(locations: List<LocationEntity>) = jdbiRepository.insertBatch(locations)
    fun deleteAll() = jdbiRepository.deleteAll()
}