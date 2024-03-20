package refraff.parser.expression;

import refraff.parser.AbstractSyntaxTreeNode;
import refraff.parser.type.Type;

import java.util.Objects;

public abstract class Expression extends AbstractSyntaxTreeNode {
    
    private static final String NODE_TYPE_DESCRIPTOR = " expression";

    private Type expressionType;

    public Expression(String detailedDescriptor) {
        this(detailedDescriptor, null);
    }

    public Expression(String detailedDescriptor, Type expressionType) {
        super(detailedDescriptor + NODE_TYPE_DESCRIPTOR);

        this.expressionType = expressionType;
    }

    public Type getExpressionType() {
        return expressionType;
    }

    public void setExpressionType(Type expressionType) {
        this.expressionType = expressionType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), expressionType);
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof Expression otherExpression
                && Objects.equals(getExpressionType(), otherExpression.getExpressionType());
    }

}
