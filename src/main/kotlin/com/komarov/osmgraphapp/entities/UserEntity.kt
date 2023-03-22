package com.komarov.osmgraphapp.entities

import java.util.UUID

data class UserEntity (
    val id: UUID,
    val login: String,
    val password: String
)