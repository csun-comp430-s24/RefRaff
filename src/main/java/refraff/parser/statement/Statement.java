package refraff.parser.statement;

import refraff.parser.AbstractSyntaxTreeNode;

public abstract class Statement extends AbstractSyntaxTreeNode {
    
    private static final String TYPE_DESCRIPTOR = "Statement";

    public Statement(String parsedValue) {
        super(parsedValue);
    }

    @Override
    public String getNodeTypeDescriptor() {
        return TYPE_DESCRIPTOR;
    }
}
