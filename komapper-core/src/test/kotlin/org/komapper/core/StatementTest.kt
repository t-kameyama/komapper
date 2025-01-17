package org.komapper.core

import kotlin.test.Test
import kotlin.test.assertEquals

internal class StatementTest {

    @Test
    fun args() {
        val statement =
            Statement(
                listOf(
                    StatementPart.Text("select * from employee where name = "),
                    StatementPart.Value(Value("aaa")),
                    StatementPart.Text(" and age = "),
                    StatementPart.Value(Value(20))
                ),
            )
        assertEquals(listOf(Value("aaa"), Value(20)), statement.args)
    }

    @Test
    fun toSql() {
        val statement =
            Statement(
                listOf(
                    StatementPart.Text("select * from employee where name = "),
                    StatementPart.Value(Value("aaa")),
                    StatementPart.Text(" and age = "),
                    StatementPart.Value(Value(20))
                ),
            )
        assertEquals("select * from employee where name = ? and age = ?", statement.toSql())
        assertEquals(
            "select * from employee where name = aaa and age = 20",
            statement.toSqlWithArgs { first, _, _ -> first.toString() }
        )
    }
}
