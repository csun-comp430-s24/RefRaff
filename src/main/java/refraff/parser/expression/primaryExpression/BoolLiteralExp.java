package refraff.parser.expression.primaryExpression;


import refraff.parser.type.BoolType;

public class BoolLiteralExp extends PrimaryExpression {

    private final boolean value;

    public BoolLiteralExp(boolean value) {
        super(String.valueOf(value), new BoolType());

        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

}
