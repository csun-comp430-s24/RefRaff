package refraff.parser;

import java.util.List;
import refraff.parser.type.Type;

public class Param extends AbstractSyntaxTreeNode {

    private static final String TYPE = "parameter";
    private static final String TYPE_DESCRIPTOR = "Parameter";

    public final Type type;
    public final Variable variable;

    public Param(final Type type, final Variable variable) {
        super(TYPE);
        this.type = type;
        this.variable = variable;
    }

    @Override
    public String getNodeTypeDescriptor() {
        return TYPE_DESCRIPTOR;
    }
}
