package refraff.parser.expression.primaryExpression;

import refraff.parser.expression.Expression;
import refraff.parser.type.Type;

public class PrimaryExpression extends Expression {
    
    private static final String TYPE_DESCRIPTOR = "Primary Expression";

    public PrimaryExpression(String parsedValue) {
        super(parsedValue);
    }

    public PrimaryExpression(String parsedValue, Type expressionType) {
        super(parsedValue, expressionType);
    }

    @Override
    public String getNodeTypeDescriptor() {
        return TYPE_DESCRIPTOR;
    }
}
