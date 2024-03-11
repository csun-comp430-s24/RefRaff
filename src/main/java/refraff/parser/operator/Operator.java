package refraff.parser.operator;

import refraff.parser.AbstractSyntaxTreeNode;

public class Operator extends AbstractSyntaxTreeNode {
    
    private static final String TYPE_DESCRIPTOR = "Operator";

    public Operator(String parsedValue) {
        super(parsedValue);
    }

    @Override
    public String getNodeTypeDescriptor() {
        return TYPE_DESCRIPTOR;
    }
}
