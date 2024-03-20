package refraff.parser.expression.primaryExpression;

import refraff.parser.type.StructType;

public class NullExp extends PrimaryExpression {

    private static final String NODE_TYPE_DESCRIPTOR = "null";

    public NullExp() {
        super(NODE_TYPE_DESCRIPTOR, new StructType(null));
    }

}
