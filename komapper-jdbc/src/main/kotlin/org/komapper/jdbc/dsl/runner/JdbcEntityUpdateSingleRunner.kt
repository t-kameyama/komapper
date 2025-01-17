package org.komapper.jdbc.dsl.runner

import org.komapper.core.DatabaseConfig
import org.komapper.core.DryRunStatement
import org.komapper.core.dsl.context.EntityUpdateContext
import org.komapper.core.dsl.metamodel.EntityMetamodel
import org.komapper.core.dsl.runner.EntityUpdateSingleRunner
import org.komapper.jdbc.JdbcDatabaseConfig

internal class JdbcEntityUpdateSingleRunner<ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>>(
    context: EntityUpdateContext<ENTITY, ID, META>,
    private val entity: ENTITY
) : JdbcRunner<ENTITY> {

    private val runner: EntityUpdateSingleRunner<ENTITY, ID, META> =
        EntityUpdateSingleRunner(context, entity)

    private val support: JdbcEntityUpdateRunnerSupport<ENTITY, ID, META> =
        JdbcEntityUpdateRunnerSupport(context)

    override fun check(config: DatabaseConfig) {
        runner.check(config)
    }

    override fun run(config: JdbcDatabaseConfig): ENTITY {
        val newEntity = preUpdate(config, entity)
        val (count) = update(config, newEntity)
        return postUpdate(newEntity, count)
    }

    private fun preUpdate(config: JdbcDatabaseConfig, entity: ENTITY): ENTITY {
        return runner.preUpdate(config, entity)
    }

    private fun update(config: JdbcDatabaseConfig, entity: ENTITY): Pair<Int, List<Long>> {
        val statement = runner.buildStatement(config, entity)
        return support.update(config) { it.executeUpdate(statement) }
    }

    private fun postUpdate(entity: ENTITY, count: Int): ENTITY {
        return runner.postUpdate(entity, count)
    }

    override fun dryRun(config: DatabaseConfig): DryRunStatement {
        return runner.dryRun(config)
    }
}
