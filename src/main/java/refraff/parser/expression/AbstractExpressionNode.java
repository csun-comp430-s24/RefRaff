package refraff.parser.expression;

import refraff.parser.AbstractSyntaxTreeNode;

public class AbstractExpressionNode extends AbstractSyntaxTreeNode {
    
    private static final String TYPE_DESCRIPTOR = "Expression";

    public AbstractExpressionNode(String parsedValue) {
        super(parsedValue);
    }

    @Override
    public String getNodeTypeDescriptor() {
        return TYPE_DESCRIPTOR;
    }
}
