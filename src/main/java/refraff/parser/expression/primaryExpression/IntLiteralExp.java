package refraff.parser.expression.primaryExpression;


import refraff.parser.type.IntType;

public class IntLiteralExp extends PrimaryExpression {
    
    private final int intLiteral;

    public IntLiteralExp(int intLiteral) {
        super(Integer.toString(intLiteral), new IntType());

        this.intLiteral = intLiteral;
    }
    
    public int getIntLiteral() {
        return intLiteral;
    }
}
