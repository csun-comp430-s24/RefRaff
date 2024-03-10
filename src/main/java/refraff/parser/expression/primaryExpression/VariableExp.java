package refraff.parser.expression.primaryExpression;


public class VariableExp extends PrimaryExpression {

    private final String name; // I don't know if we need this
    
    public VariableExp(String name) {
        super(name);
        this.name = name;
    }
}
