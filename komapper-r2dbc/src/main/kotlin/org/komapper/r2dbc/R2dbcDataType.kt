package org.komapper.r2dbc

import io.r2dbc.spi.Blob
import io.r2dbc.spi.Clob
import io.r2dbc.spi.Row
import io.r2dbc.spi.Statement
import org.komapper.core.ThreadSafe
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteBuffer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.reflect.KClass

/**
 * Represents a data type for R2DBC access.
 */
@ThreadSafe
interface R2dbcDataType<T : Any> {
    /**
     * The data type name.
     */
    val name: String

    /**
     * The corresponding class.
     */
    val klass: KClass<*>

    /**
     * Returns the value.
     *
     * @param row the row
     * @param index the index
     * @return the value
     */
    fun getValue(row: Row, index: Int): T?

    /**
     * Returns the value.
     *
     * @param row the row
     * @param columnLabel the column label
     * @return the value
     */
    fun getValue(row: Row, columnLabel: String): T?

    /**
     * Sets the value.
     *
     * @param statement the statement
     * @param index the index
     * @param value the value
     */
    fun setValue(statement: Statement, index: Int, value: T?)

    /**
     * Sets the value.
     *
     * @param statement the statement
     * @param name the name of identifier to bind to
     * @param value the value
     */
    fun setValue(statement: Statement, name: String, value: T?)

    /**
     * Returns the string presentation of the value.
     *
     * @param value the value
     * @return the string presentation of the value
     */
    fun toString(value: T?): String
}

abstract class AbstractR2dbcDataType<T : Any>(
    override val klass: KClass<T>,
    val typeOfNull: Class<*> = klass.javaObjectType
) : R2dbcDataType<T> {

    override fun getValue(row: Row, index: Int): T? {
        return row[index]?.let { convertBeforeGetting(it) }
    }

    override fun getValue(row: Row, columnLabel: String): T? {
        return row[columnLabel]?.let { convertBeforeGetting(it) }
    }

    protected open fun convertBeforeGetting(value: Any): T {
        throw UnsupportedOperationException()
    }

    override fun setValue(statement: Statement, index: Int, value: T?) {
        if (value == null) {
            statement.bindNull(index, typeOfNull)
        } else {
            bind(statement, index, value)
        }
    }

    override fun setValue(statement: Statement, name: String, value: T?) {
        if (value == null) {
            statement.bindNull(name, typeOfNull)
        } else {
            bind(statement, name, value)
        }
    }

    protected open fun bind(statement: Statement, index: Int, value: T) {
        statement.bind(index, convertBeforeBinding(value))
    }

    protected open fun bind(statement: Statement, name: String, value: T) {
        statement.bind(name, convertBeforeBinding(value))
    }

    protected open fun convertBeforeBinding(value: T): Any {
        return value
    }

    override fun toString(value: T?): String {
        return if (value == null) "null" else doToString(value)
    }

    protected open fun doToString(value: T): String {
        return value.toString()
    }
}

class R2dbcBigDecimalType(override val name: String) :
    AbstractR2dbcDataType<BigDecimal>(BigDecimal::class) {
    override fun convertBeforeGetting(value: Any): BigDecimal {
        return when (value) {
            is Double -> value.toBigDecimal()
            is Float -> value.toBigDecimal()
            is Int -> value.toBigDecimal()
            is Long -> value.toBigDecimal()
            is BigDecimal -> value
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }
}

class R2dbcBigIntegerType(override val name: String) : R2dbcDataType<BigInteger> {
    private val dataType = R2dbcBigDecimalType(name)
    override val klass: KClass<BigInteger> = BigInteger::class

    override fun getValue(row: Row, index: Int): BigInteger? {
        return dataType.getValue(row, index)?.toBigInteger()
    }

    override fun getValue(row: Row, columnLabel: String): BigInteger? {
        return dataType.getValue(row, columnLabel)?.toBigInteger()
    }

    override fun setValue(statement: Statement, index: Int, value: BigInteger?) {
        dataType.setValue(statement, index, value?.toBigDecimal())
    }

    override fun setValue(statement: Statement, name: String, value: BigInteger?) {
        dataType.setValue(statement, name, value?.toBigDecimal())
    }

    override fun toString(value: BigInteger?): String {
        return dataType.toString(value?.toBigDecimal())
    }
}

class R2dbcBlobType(override val name: String) :
    AbstractR2dbcDataType<Blob>(Blob::class) {

    override fun getValue(row: Row, index: Int): Blob? {
        return row.get(index, Blob::class.java)
    }

    override fun getValue(row: Row, columnLabel: String): Blob? {
        return row.get(columnLabel, Blob::class.java)
    }
}

class R2dbcBooleanType(override val name: String) :
    AbstractR2dbcDataType<Boolean>(Boolean::class) {

    override fun convertBeforeGetting(value: Any): Boolean {
        return when (value) {
            is Boolean -> value
            is Number -> value.toInt() == 1
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }

    override fun doToString(value: Boolean): String {
        return value.toString().uppercase()
    }
}

class R2dbcByteType(override val name: String) :
    AbstractR2dbcDataType<Byte>(Byte::class) {
    override fun convertBeforeGetting(value: Any): Byte {
        return when (value) {
            is Number -> value.toByte()
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }
}

class R2dbcByteArrayType(override val name: String) :
    AbstractR2dbcDataType<ByteArray>(ByteArray::class) {
    override fun convertBeforeGetting(value: Any): ByteArray {
        return when (value) {
            is ByteArray -> value
            is ByteBuffer -> value.array()
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }
}

class R2dbcClobType(override val name: String) :
    AbstractR2dbcDataType<Clob>(Clob::class) {

    override fun getValue(row: Row, index: Int): Clob? {
        return row.get(index, Clob::class.java)
    }

    override fun getValue(row: Row, columnLabel: String): Clob? {
        return row.get(columnLabel, Clob::class.java)
    }
}

class R2dbcDoubleType(override val name: String) :
    AbstractR2dbcDataType<Double>(Double::class) {
    override fun convertBeforeGetting(value: Any): Double {
        return when (value) {
            is Number -> value.toDouble()
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }
}

class R2dbcFloatType(override val name: String) :
    AbstractR2dbcDataType<Float>(Float::class) {
    override fun convertBeforeGetting(value: Any): Float {
        return when (value) {
            is Number -> value.toFloat()
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }
}

class R2dbcInstantType(override val name: String) : AbstractR2dbcDataType<Instant>(Instant::class) {

    override fun getValue(row: Row, index: Int): Instant? {
        return row.get(index, Instant::class.java)
    }

    override fun getValue(row: Row, columnLabel: String): Instant? {
        return row.get(columnLabel, Instant::class.java)
    }
}

class R2dbcInstantAsTimestampType(override val name: String) :
    AbstractR2dbcDataType<Instant>(Instant::class, LocalDateTime::class.java) {

    override fun convertBeforeGetting(value: Any): Instant {
        return when (value) {
            is LocalDateTime -> value.toInstant(ZoneOffset.UTC)
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }

    override fun convertBeforeBinding(value: Instant): Any {
        return LocalDateTime.ofInstant(value, ZoneOffset.UTC)
    }
}

class R2dbcInstantAsTimestampWithTimezoneType(override val name: String) :
    AbstractR2dbcDataType<Instant>(Instant::class, OffsetDateTime::class.java) {

    override fun convertBeforeGetting(value: Any): Instant {
        return when (value) {
            is LocalDateTime -> value.toInstant(ZoneOffset.UTC)
            is OffsetDateTime -> value.toInstant()
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }

    override fun convertBeforeBinding(value: Instant): Any {
        return value.atOffset(ZoneOffset.UTC)
    }
}

class R2dbcIntType(override val name: String) :
    AbstractR2dbcDataType<Int>(Int::class) {

    override fun convertBeforeGetting(value: Any): Int {
        return when (value) {
            is Number -> value.toInt()
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }
}

class R2dbcLocalDateTimeType(override val name: String) :
    AbstractR2dbcDataType<LocalDateTime>(LocalDateTime::class) {

    override fun convertBeforeGetting(value: Any): LocalDateTime {
        return when (value) {
            is LocalDateTime -> value
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }

    override fun doToString(value: LocalDateTime): String {
        return "'$value'"
    }
}

class R2dbcLocalDateType(override val name: String) :
    AbstractR2dbcDataType<LocalDate>(LocalDate::class) {

    override fun convertBeforeGetting(value: Any): LocalDate {
        return when (value) {
            is LocalDate -> value
            is LocalDateTime -> value.toLocalDate()
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }

    override fun doToString(value: LocalDate): String {
        return "'$value'"
    }
}

class R2dbcLocalTimeType(override val name: String) :
    AbstractR2dbcDataType<LocalTime>(LocalTime::class) {

    override fun convertBeforeGetting(value: Any): LocalTime {
        return when (value) {
            is LocalTime -> value
            is LocalDateTime -> value.toLocalTime()
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }

    override fun doToString(value: LocalTime): String {
        return "'$value'"
    }
}

class R2dbcLongType(override val name: String) :
    AbstractR2dbcDataType<Long>(Long::class) {
    override fun convertBeforeGetting(value: Any): Long {
        return when (value) {
            is Number -> value.toLong()
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }
}

class R2dbcOffsetDateTimeType(override val name: String) :
    AbstractR2dbcDataType<OffsetDateTime>(OffsetDateTime::class) {

    override fun convertBeforeGetting(value: Any): OffsetDateTime {
        return when (value) {
            is LocalDateTime -> {
                val zoneId = ZoneId.systemDefault()
                val offset = zoneId.rules.getOffset(value)
                value.atOffset(offset)
            }
            is OffsetDateTime -> value
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }

    override fun doToString(value: OffsetDateTime): String {
        return "'$value'"
    }
}

class R2dbcShortType(override val name: String) :
    AbstractR2dbcDataType<Short>(Short::class) {
    override fun convertBeforeGetting(value: Any): Short {
        return when (value) {
            is Number -> value.toShort()
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }
}

class R2dbcStringType(override val name: String) :
    AbstractR2dbcDataType<String>(String::class) {

    override fun convertBeforeGetting(value: Any): String {
        return when (value) {
            is String -> value
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }

    override fun doToString(value: String): String {
        return "'$value'"
    }
}

class R2dbcUByteType(override val name: String) :
    AbstractR2dbcDataType<UByte>(UByte::class, Short::class.javaObjectType) {
    override fun convertBeforeGetting(value: Any): UByte {
        return when (value) {
            is Number -> value.toLong().also {
                if (it < 0L) error("Negative value isn't convertible to UByte. value=$value")
            }.toUByte()
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }

    override fun convertBeforeBinding(value: UByte): Any {
        return value.toShort()
    }
}

class R2dbcUIntType(override val name: String) : AbstractR2dbcDataType<UInt>(UInt::class, Long::class.javaObjectType) {
    override fun convertBeforeGetting(value: Any): UInt {
        return when (value) {
            is Number -> value.toLong().also {
                if (it < 0L) error("Negative value isn't convertible to UInt. value=$value")
            }.toUInt()
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }

    override fun convertBeforeBinding(value: UInt): Any {
        return value.toLong()
    }
}

class R2dbcUShortType(override val name: String) :
    AbstractR2dbcDataType<UShort>(UShort::class, Int::class.javaObjectType) {
    override fun convertBeforeGetting(value: Any): UShort {
        return when (value) {
            is Number -> value.toLong().also {
                if (it < 0L) error("Negative value isn't convertible to UShort. value=$value")
            }.toUShort()
            else -> error("Cannot convert. value=$value, type=${value::class.qualifiedName}.")
        }
    }

    override fun convertBeforeBinding(value: UShort): Any {
        return value.toInt()
    }
}
