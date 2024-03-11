package refraff.parser.expression;

import refraff.parser.AbstractSyntaxTreeNode;

public abstract class Expression extends AbstractSyntaxTreeNode {
    
    private static final String TYPE_DESCRIPTOR = "Expression";

    public Expression(String parsedValue) {
        super(parsedValue);
    }

    @Override
    public String getNodeTypeDescriptor() {
        return TYPE_DESCRIPTOR;
    }
}
