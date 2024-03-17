package refraff.parser.type;

import refraff.parser.AbstractSyntaxTreeNode;

public abstract class Type extends AbstractSyntaxTreeNode {
    
    private static final String TYPE_DESCRIPTOR = "Type";

    public Type(String parsedValue) {
        super(parsedValue);
    }

    @Override
    public String getNodeTypeDescriptor() {
        return TYPE_DESCRIPTOR;
    }

}
