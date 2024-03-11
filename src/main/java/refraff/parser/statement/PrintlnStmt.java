package refraff.parser.statement;

import refraff.parser.expression.Expression;

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
        return expression.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PrintlnStmt otherPrintln && expression.equals(otherPrintln.expression);
    }

}
