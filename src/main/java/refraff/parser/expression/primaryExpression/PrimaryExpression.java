package refraff.parser.expression.primaryExpression;

import refraff.parser.expression.Expression;
import refraff.parser.type.Type;

public class PrimaryExpression extends Expression {
    
    private static final String NODE_TYPE_DESCRIPTOR = "primary ";

    public PrimaryExpression(String detailedDescriptor) {
        this(detailedDescriptor, null);
    }

    public PrimaryExpression(String detailedDescriptor, Type expressionType) {
        super(NODE_TYPE_DESCRIPTOR + detailedDescriptor, expressionType);
    }

}
