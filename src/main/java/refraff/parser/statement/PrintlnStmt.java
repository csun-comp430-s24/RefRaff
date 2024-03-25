package refraff.parser.statement;

import refraff.parser.expression.Expression;

import java.util.Objects;

public class PrintlnStmt extends Statement {

    private static final String PRINTLN_FORMAT = "println(%s)";

    private final Expression expression;

    public PrintlnStmt(Expression expression) {
        super(String.format(PRINTLN_FORMAT, expression.getParsedValue()));

        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), expression);
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof PrintlnStmt otherPrintln
                && Objects.equals(getExpression(), otherPrintln.getExpression());
    }

}
