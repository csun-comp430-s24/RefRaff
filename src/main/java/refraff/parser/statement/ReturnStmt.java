package refraff.parser.statement;

import refraff.parser.expression.Expression;

import java.util.Objects;
import java.util.Optional;

public class ReturnStmt extends Statement {

    private static final String NODE_TYPE_DESCRIPTOR = "return";

    private final Optional<Expression> returnValue;

    public ReturnStmt() {
        this(null);
    }

    public ReturnStmt(Expression expression) {
        super(NODE_TYPE_DESCRIPTOR);

        this.returnValue = Optional.ofNullable(expression);
    }

    public Optional<Expression> getReturnValue() {
        return returnValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), returnValue);
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof ReturnStmt otherReturn
                && Objects.equals(getReturnValue(), otherReturn.getReturnValue());
    }

}
