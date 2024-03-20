package refraff.parser.expression.primaryExpression;


import refraff.parser.type.BoolType;

import java.util.Objects;

public class BoolLiteralExp extends PrimaryExpression {

    private static final String NODE_TYPE_DESCRIPTOR = "bool literal";

    private final boolean value;

    public BoolLiteralExp(boolean value) {
        super(NODE_TYPE_DESCRIPTOR, new BoolType());

        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getValue());
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof BoolLiteralExp otherBoolLiteralExp
                && getValue() == otherBoolLiteralExp.getValue();
    }

}
