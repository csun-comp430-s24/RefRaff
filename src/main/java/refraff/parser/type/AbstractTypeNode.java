package refraff.parser.type;

import refraff.parser.AbstractSyntaxTreeNode;

public class AbstractTypeNode extends AbstractSyntaxTreeNode {
    
    private static final String TYPE_DESCRIPTOR = "Type";

    public AbstractTypeNode(String parsedValue) {
        super(parsedValue);
    }

    @Override
    public String getNodeTypeDescriptor() {
        return TYPE_DESCRIPTOR;
    }
}
