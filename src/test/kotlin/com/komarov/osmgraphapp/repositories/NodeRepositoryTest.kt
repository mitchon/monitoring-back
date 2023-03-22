package com.komarov.osmgraphapp.repositories

import com.komarov.osmgraphapp.entities.NodeEntity
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class NodeRepositoryTest(
    @Autowired
    private val repository: NodeRepository
) {
    @Test
    fun findAll() {
        val result = repository.findAll()
        Assertions.assertNotNull(result)
    }

    @Test
    fun insertBatch() {
        val values = listOf(NodeEntity(3, 1, 1.0, 1.0))
        val result = repository.insertBatch(values)
    }
}