package org.komapper.core.dsl.builder

import org.komapper.core.config.Dialect
import org.komapper.core.data.StatementBuffer
import org.komapper.core.data.Value
import org.komapper.core.dsl.context.SqlSelectContext
import org.komapper.core.dsl.element.Criterion
import org.komapper.core.dsl.element.Operand
import org.komapper.core.dsl.expr.AggregateFunction
import org.komapper.core.dsl.expr.AliasExpression
import org.komapper.core.dsl.expr.ArithmeticExpression
import org.komapper.core.dsl.expr.EntityExpression
import org.komapper.core.dsl.expr.PropertyExpression
import org.komapper.core.dsl.expr.StringFunction
import org.komapper.core.dsl.option.LikeOption

internal class BuilderSupport(
    private val dialect: Dialect,
    private val aliasManager: AliasManager,
    private val buf: StatementBuffer
) {

    fun visitEntityExpression(expression: EntityExpression<*>) {
        val name = expression.getCanonicalTableName(dialect::quote)
        val alias = aliasManager.getAlias(expression) ?: error("Alias is not found. table=$name")
        buf.append("$name $alias")
    }

    fun visitPropertyExpression(expression: PropertyExpression<*>) {
        when (expression) {
            is AggregateFunction -> {
                visitAggregateFunction(expression)
            }
            is ArithmeticExpression -> {
                visitArithmeticExpr(expression)
            }
            is AliasExpression -> {
                visitAsExpression(expression)
            }
            is StringFunction -> {
                visitStringFunction(expression)
            }
            else -> {
                val name = expression.getCanonicalColumnName(dialect::quote)
                val owner = expression.owner
                val alias = aliasManager.getAlias(owner)
                    ?: error("Alias is not found. table=${owner.getCanonicalTableName(dialect::quote)}, column=$name")
                if (alias.isBlank()) {
                    buf.append(name)
                } else {
                    buf.append("$alias.$name")
                }
            }
        }
    }

    private fun visitAggregateFunction(function: AggregateFunction<*>) {
        when (function) {
            is AggregateFunction.Avg -> {
                buf.append("avg(")
                visitPropertyExpression(function.expression)
                buf.append(")")
            }
            is AggregateFunction.CountAsterisk -> {
                buf.append("count(*)")
            }
            is AggregateFunction.Count -> {
                buf.append("count(")
                visitPropertyExpression(function.expression)
                buf.append(")")
            }
            is AggregateFunction.Max -> {
                buf.append("max(")
                visitPropertyExpression(function.expression)
                buf.append(")")
            }
            is AggregateFunction.Min<*> -> {
                buf.append("min(")
                visitPropertyExpression(function.expression)
                buf.append(")")
            }
            is AggregateFunction.Sum<*> -> {
                buf.append("sum(")
                visitPropertyExpression(function.expression)
                buf.append(")")
            }
        }
    }

    private fun visitArithmeticExpr(expression: ArithmeticExpression<*>) {
        buf.append("(")
        when (expression) {
            is ArithmeticExpression.Plus<*> -> {
                visitOperand(expression.left)
                buf.append(" + ")
                visitOperand(expression.right)
            }
            is ArithmeticExpression.Minus<*> -> {
                visitOperand(expression.left)
                buf.append(" - ")
                visitOperand(expression.right)
            }
            is ArithmeticExpression.Times<*> -> {
                visitOperand(expression.left)
                buf.append(" * ")
                visitOperand(expression.right)
            }
            is ArithmeticExpression.Div<*> -> {
                visitOperand(expression.left)
                buf.append(" / ")
                visitOperand(expression.right)
            }
            is ArithmeticExpression.Rem<*> -> {
                visitOperand(expression.left)
                buf.append(" % ")
                visitOperand(expression.right)
            }
        }.also {
            buf.append(")")
        }
    }

    private fun visitAsExpression(expression: AliasExpression<*>) {
        visitPropertyExpression(expression.expression)
        buf.append(" as ${dialect.quote(expression.alias)}")
    }

    private fun visitStringFunction(function: StringFunction) {
        buf.append("(")
        when (function) {
            is StringFunction.Concat -> {
                buf.append("concat(")
                visitOperand(function.left)
                buf.append(", ")
                visitOperand(function.right)
                buf.append(")")
            }
        }.also {
            buf.append(")")
        }
    }

    fun visitCriterion(index: Int, c: Criterion) {
        when (c) {
            is Criterion.Eq -> binaryOperation(c.left, c.right, "=")
            is Criterion.NotEq -> binaryOperation(c.left, c.right, "<>")
            is Criterion.Less -> binaryOperation(c.left, c.right, "<")
            is Criterion.LessEq -> binaryOperation(c.left, c.right, "<=")
            is Criterion.Grater -> binaryOperation(c.left, c.right, ">")
            is Criterion.GraterEq -> binaryOperation(c.left, c.right, ">=")
            is Criterion.IsNull -> isNullOperation(c.left)
            is Criterion.IsNotNull -> isNullOperation(c.left, true)
            is Criterion.Like -> likeOperation(c.left, c.right, c.option)
            is Criterion.NotLike -> likeOperation(c.left, c.right, c.option, true)
            is Criterion.Between -> betweenOperation(c.left, c.right)
            is Criterion.NotBetween -> betweenOperation(c.left, c.right, true)
            is Criterion.InList -> inListOperation(c.left, c.right)
            is Criterion.NotInList -> inListOperation(c.left, c.right, true)
            is Criterion.InSubQuery -> inSubQueryOperation(c.left, c.right)
            is Criterion.NotInSubQuery -> inSubQueryOperation(c.left, c.right, true)
            is Criterion.Exists -> existsOperation(c.context)
            is Criterion.NotExists -> existsOperation(c.context, true)
            is Criterion.And -> logicalBinaryOperation("and", c.criteria, index)
            is Criterion.Or -> logicalBinaryOperation("or", c.criteria, index)
            is Criterion.Not -> notOperation(c.criteria)
        }
    }

    private fun binaryOperation(left: Operand, right: Operand, operator: String) {
        visitOperand(left)
        buf.append(" $operator ")
        visitOperand(right)
    }

    private fun isNullOperation(left: Operand, not: Boolean = false) {
        visitOperand(left)
        val predicate = if (not) {
            " is not null"
        } else {
            " is null"
        }
        buf.append(predicate)
    }

    private fun likeOperation(left: Operand, right: Operand, option: LikeOption, not: Boolean = false) {
        visitOperand(left)
        if (not) {
            buf.append(" not")
        }
        buf.append(" like ")
        visitLikeOperand(right, option)
    }

    private fun betweenOperation(left: Operand, right: Pair<Operand, Operand>, not: Boolean = false) {
        visitOperand(left)
        if (not) {
            buf.append(" not")
        }
        buf.append(" between ")
        val (start, end) = right
        visitOperand(start)
        buf.append(" and ")
        visitOperand(end)
    }

    private fun visitLikeOperand(operand: Operand, option: LikeOption) {
        fun bind(value: Any?, mapper: (String) -> String, escape: (String) -> String) {
            if (value == null) {
                buf.bind(Value(null, String::class))
            } else {
                val text = mapper(escape(value.toString()))
                buf.bind(Value(text, String::class))
            }
        }
        when (operand) {
            is Operand.Property -> {
                visitPropertyExpression(operand.expression)
            }
            is Operand.Parameter -> {
                val value = operand.value
                val escape = dialect::escape
                when (option) {
                    is LikeOption.None -> bind(value, { it }, { it })
                    is LikeOption.Escape -> bind(value, { it }, escape)
                    is LikeOption.Prefix -> bind(value, { "$it%" }, escape)
                    is LikeOption.Infix -> bind(value, { "%$it%" }, escape)
                    is LikeOption.Suffix -> bind(value, { "%$it" }, escape)
                }
            }
        }
    }

    private fun inListOperation(left: Operand, right: List<Operand>, not: Boolean = false) {
        visitOperand(left)
        if (not) {
            buf.append(" not")
        }
        buf.append(" in (")
        if (right.isEmpty()) {
            buf.append("null")
        } else {
            for (parameter in right) {
                visitOperand(parameter)
                buf.append(", ")
            }
            buf.cutBack(2)
        }
        buf.append(")")
    }

    private fun inSubQueryOperation(left: Operand, right: SqlSelectContext<*>, not: Boolean = false) {
        visitOperand(left)
        if (not) {
            buf.append(" not")
        }
        buf.append(" in (")
        val childAliasManager = AliasManagerImpl(right, aliasManager)
        val builder = SqlSelectStatementBuilder(dialect, right, childAliasManager)
        buf.append(builder.build())
        buf.append(")")
    }

    private fun existsOperation(subContext: SqlSelectContext<*>, not: Boolean = false) {
        if (not) {
            buf.append("not ")
        }
        buf.append("exists (")
        val childAliasManager = AliasManagerImpl(subContext, aliasManager)
        val builder = SqlSelectStatementBuilder(dialect, subContext, childAliasManager)
        buf.append(builder.build())
        buf.append(")")
    }

    private fun logicalBinaryOperation(operator: String, criteria: List<Criterion>, index: Int) {
        if (criteria.isNotEmpty()) {
            if (index > 0) {
                buf.cutBack(5)
            }
            if (index != 0) {
                buf.append(" $operator ")
            }
            buf.append("(")
            for ((i, c) in criteria.withIndex()) {
                visitCriterion(i, c)
                buf.append(" and ")
            }
            buf.cutBack(5)
            buf.append(")")
        }
    }

    private fun notOperation(criteria: List<Criterion>) {
        if (criteria.isNotEmpty()) {
            buf.append("not ")
            buf.append("(")
            for ((index, c) in criteria.withIndex()) {
                visitCriterion(index, c)
                buf.append(" and ")
            }
            buf.cutBack(5)
            buf.append(")")
        }
    }

    fun visitOperand(operand: Operand) {
        when (operand) {
            is Operand.Property -> {
                visitPropertyExpression(operand.expression)
            }
            is Operand.Parameter -> {
                buf.bind(Value(operand.value, operand.expression.klass))
            }
        }
    }
}
