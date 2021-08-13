package org.komapper.core.dsl.scope

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.komapper.core.dsl.element.Criterion
import org.komapper.core.dsl.element.Operand
import org.komapper.core.dsl.metamodel.PropertyMetamodelStub
import org.komapper.core.dsl.scope.WhenScope.Companion.and
import org.komapper.core.dsl.scope.WhenScope.Companion.or
import org.komapper.core.dsl.scope.WhenScope.Companion.plus

internal class WhenScopeTest {

    @Test
    fun plus() {
        val p1 = PropertyMetamodelStub<Nothing, Int>()
        val p2 = PropertyMetamodelStub<Nothing, String>()
        val w1: WhenDeclaration = { p1.eq(1) }
        val w2: WhenDeclaration = { p2.greater("a") }
        val w3 = w1 + w2
        val scope = WhenScope().apply(w3)
        assertEquals(2, scope.size)
        assertEquals(Criterion.Eq(Operand.Column(p1), Operand.Argument(p1, 1)), scope[0])
        assertEquals(Criterion.Grater(Operand.Column(p2), Operand.Argument(p2, "a")), scope[1])
    }

    @Test
    fun and() {
        val p1 = PropertyMetamodelStub<Nothing, Int>()
        val p2 = PropertyMetamodelStub<Nothing, String>()
        val w1: WhenDeclaration = { p1.eq(1) }
        val w2: WhenDeclaration = { p2.greater("a") }
        val w3 = w1 and w2
        val scope = WhenScope().apply(w3)
        assertEquals(2, scope.size)
        assertEquals(Criterion.Eq(Operand.Column(p1), Operand.Argument(p1, 1)), scope[0])
        assertEquals(Criterion.And(listOf(Criterion.Grater(Operand.Column(p2), Operand.Argument(p2, "a")))), scope[1])
    }

    @Test
    fun or() {
        val p1 = PropertyMetamodelStub<Nothing, Int>()
        val p2 = PropertyMetamodelStub<Nothing, String>()
        val w1: WhenDeclaration = { p1.eq(1) }
        val w2: WhenDeclaration = { p2.greater("a") }
        val w3 = w1 or w2
        val scope = WhenScope().apply(w3)
        assertEquals(2, scope.size)
        assertEquals(Criterion.Eq(Operand.Column(p1), Operand.Argument(p1, 1)), scope[0])
        assertEquals(Criterion.Or(listOf(Criterion.Grater(Operand.Column(p2), Operand.Argument(p2, "a")))), scope[1])
    }
}