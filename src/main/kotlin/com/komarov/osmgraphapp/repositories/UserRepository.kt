package com.komarov.osmgraphapp.repositories

import com.komarov.osmgraphapp.entities.UserEntity
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.sqlobject.config.RegisterBeanMapper
import org.jdbi.v3.sqlobject.kotlin.BindKotlin
import org.jdbi.v3.sqlobject.kotlin.RegisterKotlinMapper
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.springframework.stereotype.Repository
import java.util.*


@RegisterKotlinMapper(UserEntity::class)
interface UserEntityJdbiRepository {
    @SqlQuery("select * from master.users")
    fun findAll(): List<UserEntity>
}

@Repository
class UserRepository(
    private val jdbi: Jdbi,
) {
    private var jdbiRepository = jdbi.onDemand(UserEntityJdbiRepository::class.java)
    fun findAll() = jdbiRepository.findAll()
}