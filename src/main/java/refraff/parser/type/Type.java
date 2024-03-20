package refraff.parser.type;

import refraff.parser.AbstractSyntaxTreeNode;

public abstract class Type extends AbstractSyntaxTreeNode {

    public Type(String nodeTypeDescriptor) {
        super(nodeTypeDescriptor);
    }

    public boolean shouldThrowOnAssignment() {
        return true;
    }

}
