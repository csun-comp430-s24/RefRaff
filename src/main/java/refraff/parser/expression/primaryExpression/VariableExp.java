package refraff.parser.expression.primaryExpression;


import java.util.Objects;

public class VariableExp extends PrimaryExpression {

    private static final String NODE_TYPE_DESCRIPTOR = "variable";

    private final String name; // I don't know if we need this
    
    public VariableExp(String name) {
        super(NODE_TYPE_DESCRIPTOR);

        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getName());
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof VariableExp otherVariableExp
                && Objects.equals(getName(), otherVariableExp.getName());
    }

}
