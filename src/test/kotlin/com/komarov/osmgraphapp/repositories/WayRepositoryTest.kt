package com.komarov.osmgraphapp.repositories

import com.komarov.osmgraphapp.entities.WayEntity
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class WayRepositoryTest(
    @Autowired
    private val repository: WayRepository
) {
    @Test
    fun findAll() {
        val result = repository.findAll()
        Assertions.assertNotNull(result)
    }

    @Test
    fun insertBatch() {
        val values = listOf(WayEntity(id = 3))
        repository.insertBatch(values)
    }
}