package integration.jdbc

import integration.Address
import integration.meta
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.komapper.core.dsl.SqlDsl
import org.komapper.core.dsl.desc
import org.komapper.jdbc.Database

@ExtendWith(Env::class)
class SqlSelectQueryForUpdateTest(private val db: Database) {

    @Test
    fun forUpdate() {
        val a = Address.meta
        val list = db.runQuery {
            SqlDsl.from(a).where { a.addressId greaterEq 1 }
                .orderBy(a.addressId.desc())
                .limit(2)
                .offset(5)
                .forUpdate()
        }
        assertEquals(
            listOf(
                Address(10, "STREET 10", 1),
                Address(9, "STREET 9", 1)
            ),
            list
        )
    }
}