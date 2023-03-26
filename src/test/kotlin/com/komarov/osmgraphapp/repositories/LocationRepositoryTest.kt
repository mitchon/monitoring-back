package com.komarov.osmgraphapp.repositories

import com.komarov.osmgraphapp.entities.LocationEntity
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class LocationRepositoryTest(
    @Autowired
    private val repository: LocationRepository
) {
    @Test
    fun findAll() {
        val result = repository.findAll()
        Assertions.assertNotNull(result)
    }

    @Test
    fun insertBatch() {
        val values = listOf(LocationEntity(1, 1.0, 1.0), LocationEntity(2, 1.0, 1.0))
        val result = repository.insertBatch(values)
        Assertions.assertNotNull(result)
    }

    @Test
    fun deleteAll() {
        repository.deleteAll()
    }
}