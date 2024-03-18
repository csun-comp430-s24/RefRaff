package refraff.parser.expression.primaryExpression;

import refraff.parser.type.StructType;

public class NullExp extends PrimaryExpression {

    private static final String NULL = "null";

    public NullExp() {
        super(NULL, new StructType(null));
    }

}
