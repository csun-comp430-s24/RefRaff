package refraff.parser.expression.primaryExpression;

import refraff.parser.Variable;

import java.util.Objects;

public class VariableExp extends PrimaryExpression {

    private static final String NODE_TYPE_DESCRIPTOR = "variable";

    private final Variable var;
    
    public VariableExp(Variable var) {
        super(NODE_TYPE_DESCRIPTOR);

        this.var = var;
    }

    public Variable getVar() {
        return var;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getVar());
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof VariableExp otherVariableExp
                && Objects.equals(getVar(), otherVariableExp.getVar());
    }

}
