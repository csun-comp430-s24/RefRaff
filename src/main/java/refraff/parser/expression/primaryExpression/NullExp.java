package refraff.parser.expression.primaryExpression;

import refraff.Source;
import refraff.parser.Node;
import refraff.parser.type.StructType;

public class NullExp extends PrimaryExpression {

    private static final String NODE_TYPE_DESCRIPTOR = "null";

    public NullExp() {
        super(NODE_TYPE_DESCRIPTOR, new StructType(null));
    }

    @Override
    public Node setSource(Source source) throws IllegalStateException {
        getExpressionType().setSource(source);
        return super.setSource(source);
    }

}
