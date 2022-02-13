package org.komapper.jdbc.dsl.runner

import org.komapper.core.DatabaseConfig
import org.komapper.core.DryRunStatement
import org.komapper.core.dsl.context.RelationInsertSelectContext
import org.komapper.core.dsl.metamodel.EntityMetamodel
import org.komapper.core.dsl.metamodel.hasAutoIncrementProperty
import org.komapper.core.dsl.runner.RelationInsertSelectRunner
import org.komapper.jdbc.JdbcDatabaseConfig
import org.komapper.jdbc.JdbcExecutor

internal class JdbcRelationInsertSelectRunner<ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>>(
    private val context: RelationInsertSelectContext<ENTITY, ID, META>,
) : JdbcRunner<Pair<Int, List<ID>>> {

    private val runner: RelationInsertSelectRunner<ENTITY, ID, META> = RelationInsertSelectRunner(context)

    override fun check(config: DatabaseConfig) {
        runner.check(config)
    }

    override fun run(config: JdbcDatabaseConfig): Pair<Int, List<ID>> {
        val statement = runner.buildStatement(config)
        val requiresGeneratedKeys = context.target.hasAutoIncrementProperty() && context.options.returnGeneratedKeys
        val executor = JdbcExecutor(config, context.options, requiresGeneratedKeys)
        val (count, keys) = executor.executeUpdate(statement)
        val ids = keys.mapNotNull { context.target.convertToId(it) }
        return count to ids
    }

    override fun dryRun(config: DatabaseConfig): DryRunStatement {
        return runner.dryRun(config)
    }
}
