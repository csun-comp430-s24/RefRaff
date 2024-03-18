package refraff.parser.struct;

import refraff.parser.AbstractSyntaxTreeNode;
import refraff.parser.Variable;
import refraff.parser.type.Type;

public class Param extends AbstractSyntaxTreeNode {

    private static final String TYPE = "parameter";
    private static final String TYPE_DESCRIPTOR = "parameter";

    public final Type type;
    public final Variable variable;

    public Param(final Type type, final Variable variable) {
        super(TYPE);
        this.type = type;
        this.variable = variable;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String getNodeTypeDescriptor() {
        return TYPE_DESCRIPTOR;
    }
}
