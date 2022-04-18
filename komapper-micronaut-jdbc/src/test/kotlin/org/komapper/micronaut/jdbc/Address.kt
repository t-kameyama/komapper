package org.komapper.micronaut.jdbc

import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperVersion

@KomapperEntity
data class Address(
    @KomapperId
    val addressId: Int,
    val street: String,
    @KomapperVersion
    val version: Int,
)
