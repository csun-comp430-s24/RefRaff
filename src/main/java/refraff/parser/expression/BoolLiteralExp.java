package refraff.parser.expression;

public class BoolLiteralExp extends Expression {

    private final boolean value;

    public BoolLiteralExp(boolean value) {
        super(String.valueOf(value));

        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

}
