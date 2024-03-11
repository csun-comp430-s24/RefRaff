package refraff.parser.expression.primaryExpression;

import refraff.parser.expression.Expression;

public class PrimaryExpression extends Expression {
    
    private static final String TYPE_DESCRIPTOR = "Primary Expression";

    public PrimaryExpression(String parsedValue) {
        super(parsedValue);
    }

    @Override
    public String getNodeTypeDescriptor() {
        return TYPE_DESCRIPTOR;
    }
}
