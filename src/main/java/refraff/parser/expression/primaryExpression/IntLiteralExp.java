package refraff.parser.expression.primaryExpression;


import refraff.Source;
import refraff.parser.Node;
import refraff.parser.type.IntType;

import java.util.Objects;

public class IntLiteralExp extends PrimaryExpression {

    private static final String NODE_TYPE_DESCRIPTOR = "int literal";
    
    private final int intLiteral;

    public IntLiteralExp(int intLiteral) {
        super(NODE_TYPE_DESCRIPTOR, new IntType());

        this.intLiteral = intLiteral;
    }

    @Override
    public Node setSource(Source source) throws IllegalStateException {
        getExpressionType().setSource(source);
        return super.setSource(source);
    }
    
    public int getIntLiteral() {
        return intLiteral;
    }

    @Override
    public String toString() {
        return Integer.toString(intLiteral);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), intLiteral);
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof IntLiteralExp otherIntLiteralExp
                && getIntLiteral() == otherIntLiteralExp.getIntLiteral();
    }

}
