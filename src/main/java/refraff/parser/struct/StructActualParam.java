package refraff.parser.struct;

import refraff.parser.AbstractSyntaxTreeNode;
import refraff.parser.expression.Expression;
import refraff.parser.Variable;

public class StructActualParam extends AbstractSyntaxTreeNode {
        
    private static final String TYPE = "parameter";
    private static final String TYPE_DESCRIPTOR = "struct actual parameter";

    public final Variable var;
    public final Expression exp;

    public StructActualParam(Variable var, Expression exp) {
        super(TYPE);
        this.var = var;
        this.exp = exp;
    }

    @Override
    public String getNodeTypeDescriptor() {
        return TYPE_DESCRIPTOR;
    }
}
