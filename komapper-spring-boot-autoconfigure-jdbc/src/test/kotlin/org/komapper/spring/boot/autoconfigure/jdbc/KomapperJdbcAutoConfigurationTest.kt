package org.komapper.spring.boot.autoconfigure.jdbc

import org.komapper.core.ClockProvider
import org.komapper.core.ExecutionOptions
import org.komapper.core.Statement
import org.komapper.core.TemplateBuiltinExtensions
import org.komapper.core.TemplateStatementBuilder
import org.komapper.core.Value
import org.komapper.dialect.h2.jdbc.JdbcH2Dialect
import org.komapper.jdbc.JdbcDataType
import org.komapper.jdbc.JdbcDataTypeProvider
import org.komapper.jdbc.JdbcDatabase
import org.komapper.jdbc.JdbcStringType
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.MapPropertySource
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KomapperJdbcAutoConfigurationTest {

    private val context = AnnotationConfigApplicationContext()

    @Test
    fun defaultConfiguration() {
        val source = mapOf("spring.datasource.url" to "jdbc:h2:mem:example")
        val sources = context.environment.propertySources
        sources.addFirst(MapPropertySource("test", source))
        context.register(
            KomapperJdbcAutoConfiguration::class.java,
            DataSourceAutoConfiguration::class.java
        )
        context.refresh()

        val database = context.getBean(JdbcDatabase::class.java)
        assertNotNull(database)
        assertTrue(database.config.dialect is JdbcH2Dialect)
        assertNotNull(database.config.templateStatementBuilder)
    }

    @Test
    fun customConfiguration() {
        val source = mapOf("spring.datasource.url" to "jdbc:h2:mem:example")
        val sources = context.environment.propertySources
        sources.addFirst(MapPropertySource("test", source))
        context.register(
            CustomConfigure::class.java,
            KomapperJdbcAutoConfiguration::class.java,
            DataSourceAutoConfiguration::class.java
        )
        context.refresh()

        val database = context.getBean(JdbcDatabase::class.java)
        assertNotNull(database)
        val dataType = database.config.dataOperator.getDataType(String::class)
        assertEquals("abc", dataType.name)
        val clock = database.config.clockProvider.now()
        val timestamp = LocalDateTime.now(clock)
        assertEquals(LocalDateTime.of(2021, 4, 25, 16, 17, 18), timestamp)
        val jdbcOption = database.config.executionOptions
        assertEquals(1234, jdbcOption.queryTimeoutSeconds)
    }

    @Test
    fun templateStatementBuilderConfiguration() {
        val source = mapOf("spring.datasource.url" to "jdbc:h2:mem:example")
        val sources = context.environment.propertySources
        sources.addFirst(MapPropertySource("test", source))
        context.register(
            TemplateStatementBuilderConfigure::class.java,
            KomapperJdbcAutoConfiguration::class.java,
            DataSourceAutoConfiguration::class.java
        )
        context.refresh()

        val database = context.getBean(JdbcDatabase::class.java)
        assertNotNull(database)
        val builder = database.config.templateStatementBuilder
        assertTrue(builder is MyStatementBuilder)
    }

    @Suppress("unused", "UNCHECKED_CAST")
    @Configuration
    open class CustomConfigure {

        @Bean
        open fun dataTypeProvider(): JdbcDataTypeProvider {
            return object : JdbcDataTypeProvider {
                private val map: Map<KClass<*>, JdbcDataType<*>> =
                    listOf(JdbcStringType("abc")).associateBy { it.klass }

                override fun <T : Any> get(klass: KClass<out T>): JdbcDataType<T>? {
                    return map[klass] as JdbcDataType<T>?
                }
            }
        }

        @Bean
        open fun clockProvider(): ClockProvider {
            return ClockProvider {
                val timestamp = LocalDateTime.of(2021, 4, 25, 16, 17, 18)
                Clock.fixed(timestamp.toInstant(ZoneOffset.UTC), ZoneId.of("UTC"))
            }
        }

        @Bean
        open fun jdbcOption(): ExecutionOptions {
            return ExecutionOptions(queryTimeoutSeconds = 1234)
        }
    }

    @Suppress("unused")
    @Configuration
    open class TemplateStatementBuilderConfigure {
        @Bean
        open fun templateStatementBuilder(): TemplateStatementBuilder {
            return MyStatementBuilder()
        }
    }

    class MyStatementBuilder : TemplateStatementBuilder {
        override fun build(
            template: CharSequence,
            valueMap: Map<String, Value<*>>,
            builtinExtensions: TemplateBuiltinExtensions
        ): Statement {
            throw UnsupportedOperationException()
        }

        override fun clearCache() = Unit
    }
}
