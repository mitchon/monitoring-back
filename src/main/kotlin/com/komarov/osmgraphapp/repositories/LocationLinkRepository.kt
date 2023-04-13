package com.komarov.osmgraphapp.repositories

import com.komarov.osmgraphapp.entities.LocationLinkEntity
import com.komarov.osmgraphapp.utils.LocationsUpdateEvent
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlBatch
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Repository
import java.util.*


interface LocationLinkEntityJdbiRepository {
    @RegisterKotlinMapper(LocationLinkEntity::class)
    @SqlBatch("insert into master.location_links values (:start, :finish, :length)")
    fun insertBatch(@BindBean nodes: List<LocationLinkEntity>)
    @RegisterKotlinMapper(LocationLinkEntity::class)
    @SqlQuery("select * from master.location_links")
    fun findAll(): List<LocationLinkEntity>

    @RegisterKotlinMapper(LocationLinkEntity::class)
    @SqlQuery("select * from master.location_links where start = ?")
    fun findByStartId(startId: Long): List<LocationLinkEntity>

    @RegisterKotlinMapper(LocationLinkEntity::class)
    @SqlUpdate("delete from master.location_links where true")
    fun deleteAll()
}

@Repository
class LocationLinkRepository(
    private val jdbi: Jdbi,
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    private var jdbiRepository = jdbi.onDemand(LocationLinkEntityJdbiRepository::class.java)
    fun findAll() = jdbiRepository.findAll()
    fun findByStartId(startId: Long) = jdbiRepository.findByStartId(startId)
    fun insertBatch(links: List<LocationLinkEntity>) {
        jdbiRepository.insertBatch(links)
        applicationEventPublisher.publishEvent(LocationsUpdateEvent())
    }
    fun deleteAll() = jdbiRepository.deleteAll()
}