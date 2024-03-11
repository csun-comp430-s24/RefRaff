package refraff.parser.expression.primaryExpression;


public class BoolLiteralExp extends PrimaryExpression {

    private final boolean value;

    public BoolLiteralExp(boolean value) {
        super(String.valueOf(value));

        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

}
