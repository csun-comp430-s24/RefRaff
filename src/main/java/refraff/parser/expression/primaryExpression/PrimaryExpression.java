package refraff.parser.expression.primaryExpression;

import refraff.parser.AbstractSyntaxTreeNode;


public class PrimaryExpression extends AbstractSyntaxTreeNode {
    
    private static final String TYPE_DESCRIPTOR = "Primary Expression";

    public PrimaryExpression(String parsedValue) {
        super(parsedValue);
    }

    @Override
    public String getNodeTypeDescriptor() {
        return TYPE_DESCRIPTOR;
    }
}
