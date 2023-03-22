package com.komarov.osmgraphapp.repositories

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class UserRepositoryTest(
    @Autowired
    private val repository: UserRepository
) {
    @Test
    fun findAll() {
        val result = repository.findAll()
        Assertions.assertNotNull(result)
    }
}