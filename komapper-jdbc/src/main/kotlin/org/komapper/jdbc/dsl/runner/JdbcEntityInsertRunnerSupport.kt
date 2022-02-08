package org.komapper.jdbc.dsl.runner

import org.komapper.core.dsl.context.EntityInsertContext
import org.komapper.core.dsl.metamodel.EntityMetamodel
import org.komapper.core.dsl.metamodel.IdGenerator
import org.komapper.jdbc.JdbcDatabaseConfig
import org.komapper.jdbc.JdbcExecutor

internal class JdbcEntityInsertRunnerSupport<ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>>(
    private val context: EntityInsertContext<ENTITY, ID, META>,
) {

    fun preInsert(config: JdbcDatabaseConfig, entity: ENTITY): ENTITY {
        val newEntity = when (val idGenerator = context.target.idGenerator()) {
            is IdGenerator.Sequence<ENTITY, ID> ->
                if (!context.target.disableSequenceAssignment() && !context.options.disableSequenceAssignment) {
                    val id = idGenerator.execute(config, context.options)
                    idGenerator.property.setter(entity, id)
                } else null
            else -> null
        }
        val clock = config.clockProvider.now()
        return context.target.preInsert(newEntity ?: entity, clock)
    }

    fun <T> insert(config: JdbcDatabaseConfig, execute: (JdbcExecutor) -> T): T {
        val requiresGeneratedKeys = context.target.idGenerator() is IdGenerator.AutoIncrement<ENTITY, *>
        val executor = JdbcExecutor(config, context.options, requiresGeneratedKeys)
        return execute(executor)
    }

    fun postInsert(entity: ENTITY, generatedKey: Long): ENTITY {
        val idGenerator = context.target.idGenerator()
        return if (idGenerator is IdGenerator.AutoIncrement<ENTITY, ID>) {
            val id = context.target.convertToId(generatedKey)!!
            idGenerator.property.setter(entity, id)
        } else {
            entity
        }
    }
}