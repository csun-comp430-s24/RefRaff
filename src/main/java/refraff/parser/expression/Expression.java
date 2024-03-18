package refraff.parser.expression;

import refraff.parser.AbstractSyntaxTreeNode;
import refraff.parser.type.Type;

public abstract class Expression extends AbstractSyntaxTreeNode {
    
    private static final String TYPE_DESCRIPTOR = "Expression";

    private Type expressionType;

    public Expression(String parsedValue) {
        this(parsedValue, null);
    }

    public Expression(String parsedValue, Type expressionType) {
        super(parsedValue);

        this.expressionType = expressionType;
    }

    public Type getExpressionType() {
        return expressionType;
    }

    public void setExpressionType(Type expressionType) {
        this.expressionType = expressionType;
    }

    @Override
    public String getNodeTypeDescriptor() {
        return TYPE_DESCRIPTOR;
    }
}
