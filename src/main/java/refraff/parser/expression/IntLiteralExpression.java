package refraff.parser.expression;

public class IntLiteralExpression extends Expression {
    
    private final int num;

    public IntLiteralExpression(int num) {
        super(Integer.toString(num));
        this.num = num;
    }
}
