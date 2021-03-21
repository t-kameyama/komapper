package org.komapper.core.jdbc

import java.io.PrintWriter
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLFeatureNotSupportedException
import java.util.logging.Logger
import javax.sql.DataSource

@Suppress("MemberVisibilityCanBePrivate")
class SimpleDataSource(val url: String, val user: String = "", val password: String = "") : DataSource {

    override fun setLogWriter(out: PrintWriter?) {
        throw SQLFeatureNotSupportedException()
    }

    override fun setLoginTimeout(seconds: Int) {
        throw SQLFeatureNotSupportedException()
    }

    override fun isWrapperFor(iface: Class<*>?): Boolean {
        throw SQLFeatureNotSupportedException()
    }

    override fun <T : Any?> unwrap(iface: Class<T>?): T {
        throw SQLFeatureNotSupportedException()
    }

    override fun getConnection(): Connection {
        return DriverManager.getConnection(url, user, password)
    }

    override fun getConnection(username: String?, password: String?): Connection {
        throw SQLFeatureNotSupportedException()
    }

    override fun getParentLogger(): Logger {
        throw SQLFeatureNotSupportedException()
    }

    override fun getLogWriter(): PrintWriter {
        throw SQLFeatureNotSupportedException()
    }

    override fun getLoginTimeout(): Int {
        throw SQLFeatureNotSupportedException()
    }
}
