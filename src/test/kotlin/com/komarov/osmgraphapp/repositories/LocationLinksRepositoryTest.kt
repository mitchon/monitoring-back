package com.komarov.osmgraphapp.repositories

import com.komarov.osmgraphapp.entities.LocationEntity
import com.komarov.osmgraphapp.entities.LocationLinkEntity
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class LocationLinksRepositoryTest(
    @Autowired
    private val repository: LocationLinkRepository
) {
    @Test
    fun findAll() {
        val result = repository.findAll()
        Assertions.assertNotNull(result)
    }

    @Test
    fun insertBatch() {
        val values = listOf(LocationLinkEntity(1, 2, 1.0))
        val result = repository.insertBatch(values)
        Assertions.assertNotNull(result)
    }

    @Test
    fun deleteAll() {
        repository.deleteAll()
    }
}