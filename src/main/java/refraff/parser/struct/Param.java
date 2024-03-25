package refraff.parser.struct;

import refraff.parser.AbstractSyntaxTreeNode;
import refraff.parser.Variable;
import refraff.parser.type.Type;

import java.util.Objects;

public class Param extends AbstractSyntaxTreeNode {

    private static final String NODE_TYPE_DESCRIPTOR = "parameter";

    public final Type type;
    public final Variable variable;

    public Param(final Type type, final Variable variable) {
        super(NODE_TYPE_DESCRIPTOR);

        this.type = type;
        this.variable = variable;
    }

    public Type getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), type, variable);
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof Param otherParam
                && Objects.equals(type, otherParam.type)
                && Objects.equals(variable, otherParam.variable);
    }

}
