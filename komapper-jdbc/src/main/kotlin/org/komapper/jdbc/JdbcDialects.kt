package org.komapper.jdbc

import org.komapper.core.spi.findByPriority
import org.komapper.jdbc.spi.JdbcDialectFactory
import java.util.ServiceLoader
import java.util.regex.Pattern

/**
 * The provider of [JdbcDialect].
 */
object JdbcDialects {
    private val jdbcUrlPattern = Pattern.compile("^jdbc:(tc:)?([^:]*):.*")

    /**
     * @param driver the driver name
     * @return the [JdbcDialect]
     */
    fun get(driver: String): JdbcDialect {
        val loader = ServiceLoader.load(JdbcDialectFactory::class.java)
        val factory = loader.filter { it.supports(driver) }.findByPriority()
            ?: error(
                "The dialect is not found for the JDBC url. " +
                    "Try to add the 'komapper-dialect-$driver-jdbc' dependency. " +
                    "driver='$driver'"
            )
        return factory.create()
    }

    /**
     * @param url the JDBC url
     */
    fun getByUrl(url: String): JdbcDialect {
        val driver = extractJdbcDriver(url)
        return get(driver)
    }

    /**
     * @param url the JDBC url
     */
    fun extractJdbcDriver(url: String): String {
        val matcher = jdbcUrlPattern.matcher(url)
        if (matcher.matches()) {
            return matcher.group(2).lowercase()
        }
        error("The driver in the JDBC URL is not found. url=$url")
    }
}
