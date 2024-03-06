package refraff.parser.expression;

public class IntLiteralExp extends Expression {
    
    private final int num;

    public IntLiteralExp(int num) {
        super(Integer.toString(num));
        this.num = num;
    }
}
