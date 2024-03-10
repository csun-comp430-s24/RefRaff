package refraff.parser.expression.primaryExpression;


public class IntLiteralExp extends PrimaryExpression {
    
    private final int num;

    public IntLiteralExp(int num) {
        super(Integer.toString(num));
        this.num = num;
    }
    
}
