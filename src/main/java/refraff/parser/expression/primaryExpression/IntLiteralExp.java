package refraff.parser.expression.primaryExpression;


public class IntLiteralExp extends PrimaryExpression {
    
    private final int intLiteral;

    public IntLiteralExp(int intLiteral) {
        super(Integer.toString(intLiteral));
        this.intLiteral = intLiteral;
    }
    
    public int getIntLiteral() {
        return intLiteral;
    }
}
