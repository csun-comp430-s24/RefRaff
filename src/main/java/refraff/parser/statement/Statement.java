package refraff.parser.statement;

import refraff.parser.AbstractSyntaxTreeNode;

public abstract class Statement extends AbstractSyntaxTreeNode {
    
    private static final String NODE_TYPE_DESCRIPTOR = " statement";

    public Statement(String statementDescriptor) {
        super(statementDescriptor + NODE_TYPE_DESCRIPTOR);
    }

}
