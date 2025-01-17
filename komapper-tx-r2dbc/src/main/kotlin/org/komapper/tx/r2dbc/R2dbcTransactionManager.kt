package org.komapper.tx.r2dbc

import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.reactive.asFlow
import org.komapper.core.LoggerFacade
import org.komapper.core.ThreadSafe
import org.komapper.tx.core.EmptyTransactionProperty
import org.komapper.tx.core.TransactionProperty
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/**
 * The R2DBC transaction APIs designed for advanced use.
 */
@ThreadSafe
interface R2dbcTransactionManager {

    suspend fun getConnection(): Connection

    /**
     * This function must not throw any exceptions.
     */
    suspend fun isActive(): Boolean

    /**
     * This function must not throw any exceptions.
     */
    suspend fun isRollbackOnly(): Boolean

    /**
     * This function must not throw any exceptions.
     */
    suspend fun setRollbackOnly()

    suspend fun begin(transactionProperty: TransactionProperty = EmptyTransactionProperty): CoroutineContext

    suspend fun commit()

    suspend fun suspend(): CoroutineContext

    suspend fun resume()

    /**
     * This function must not throw any exceptions.
     */
    suspend fun rollback()
}

internal class R2dbcTransactionManagerImpl(
    private val connectionFactory: ConnectionFactory,
    private val loggerFacade: LoggerFacade
) : R2dbcTransactionManager {

    override suspend fun getConnection(): Connection {
        val txContext = coroutineContext[TxHolder]
        return if (txContext?.tx != null) {
            txContext.tx.connection
        } else {
            connectionFactory.create().asFlow().single()
        }
    }

    override suspend fun isActive(): Boolean {
        val txContext = coroutineContext[TxHolder]
        return txContext?.tx != null
    }

    override suspend fun isRollbackOnly(): Boolean {
        val txContext = coroutineContext[TxHolder]
        return if (txContext?.tx != null) {
            txContext.tx.isRollbackOnly
        } else false
    }

    override suspend fun setRollbackOnly() {
        val txContext = coroutineContext[TxHolder]
        if (txContext?.tx != null) {
            txContext.tx.isRollbackOnly = true
        }
    }

    override suspend fun begin(transactionProperty: TransactionProperty): CoroutineContext {
        val currentTxHolder = coroutineContext[TxHolder]
        if (currentTxHolder?.tx != null) {
            rollbackInternal(currentTxHolder.tx)
            error("The transaction \"${currentTxHolder.tx}\" already has begun.")
        }
        val tx = connectionFactory.create().asFlow().map { con ->
            val txCon = R2dbcTransactionConnectionImpl(con)
            val name = transactionProperty[TransactionProperty.Name]
            R2dbcTransactionImpl(name?.value, txCon)
        }.single()
        val begin = if (transactionProperty == EmptyTransactionProperty) {
            tx.connection.beginTransaction().asFlow()
        } else {
            val definition = transactionProperty.asDefinition()
            tx.connection.beginTransaction(definition).asFlow()
        }
        begin.onCompletion { cause ->
            if (cause == null) {
                runCatching {
                    loggerFacade.begin(tx.toString())
                }.onFailure {
                    release(tx)
                }.getOrThrow()
            } else {
                release(tx)
            }
        }.collect()
        return TxHolder(tx)
    }

    override suspend fun commit() {
        val txHolder = coroutineContext[TxHolder]
        if (txHolder?.tx == null) {
            error("A transaction hasn't yet begun.")
        }
        val connection = txHolder.tx.connection
        connection.commitTransaction().asFlow()
            .onCompletion { cause ->
                release(txHolder.tx)
                if (cause == null) {
                    loggerFacade.commit(txHolder.tx.toString())
                } else {
                    runCatching {
                        loggerFacade.commitFailed(txHolder.tx.toString(), cause)
                    }.onFailure {
                        cause.addSuppressed(it)
                    }
                }
            }.collect()
    }

    override suspend fun suspend(): CoroutineContext {
        val txHolder = coroutineContext[TxHolder]
        if (txHolder?.tx == null) {
            error("A transaction hasn't yet begun.")
        }
        loggerFacade.suspend(txHolder.tx.toString())
        return TxHolder(null)
    }

    override suspend fun resume() {
        val txHolder = coroutineContext[TxHolder]
        if (txHolder?.tx == null) {
            error("A transaction is not found.")
        }
        loggerFacade.resume(txHolder.tx.toString())
    }

    override suspend fun rollback() {
        val txHolder = coroutineContext[TxHolder]
        if (txHolder?.tx == null) {
            return
        }
        rollbackInternal(txHolder.tx)
    }

    /**
     * This function must not throw any exceptions.
     */
    private suspend fun rollbackInternal(tx: R2dbcTransaction) {
        val connection = tx.connection
        connection.rollbackTransaction().asFlow()
            .onCompletion { cause ->
                release(tx)
                runCatching {
                    if (cause == null) {
                        loggerFacade.rollback(tx.toString())
                    } else {
                        loggerFacade.rollbackFailed(tx.toString(), cause)
                    }
                }
            }.collect()
    }

    /**
     * This function must not throw any exceptions.
     */
    private suspend fun release(tx: R2dbcTransaction) {
        runCatching {
            tx.connection.dispose()
        }
    }
}

private data class TxHolder(val tx: R2dbcTransaction?) : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<TxHolder>

    override val key: CoroutineContext.Key<TxHolder> = Key
}
