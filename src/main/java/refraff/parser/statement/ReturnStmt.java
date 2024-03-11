package refraff.parser.statement;

import refraff.parser.expression.Expression;

import java.util.Optional;

public class ReturnStmt extends Statement {

    private static final String RETURN_FORMAT = "return %s;";

    private final Optional<Expression> returnValue;

    public ReturnStmt() {
        this(null);
    }

    public ReturnStmt(Expression expression) {
        super(String.format(RETURN_FORMAT, expression == null ? "" : expression.getParsedValue()));

        this.returnValue = Optional.ofNullable(expression);
    }

    public Optional<Expression> getReturnValue() {
        return returnValue;
    }

    @Override
    public int hashCode() {
        return returnValue.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ReturnStmt otherReturn && returnValue.equals(otherReturn.returnValue);
    }

}
