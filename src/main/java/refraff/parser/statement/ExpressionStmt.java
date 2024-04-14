package refraff.parser.statement;

import refraff.parser.expression.Expression;

import java.util.Objects;

public class ExpressionStmt extends Statement {

    private static final String NODE_TYPE_DESCRIPTOR = "expression";

    private final Expression expression;

    public ExpressionStmt(Expression expression) {
        super(NODE_TYPE_DESCRIPTOR);

        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getExpression());
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof ExpressionStmt otherExpressionStmt
                && Objects.equals(getExpression(), otherExpressionStmt.getExpression());
    }
}
