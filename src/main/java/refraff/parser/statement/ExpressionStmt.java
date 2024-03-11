package refraff.parser.statement;

import refraff.parser.expression.Expression;

public class ExpressionStmt extends Statement {

    private static final String EXPRESSION_FORMAT = "%s;";

    private final Expression expression;

    public ExpressionStmt(Expression expression) {
        super(expression.getParsedValue());

        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public int hashCode() {
        return expression.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ExpressionStmt otherExpressionStmt && expression.equals(otherExpressionStmt.expression);
    }
}
