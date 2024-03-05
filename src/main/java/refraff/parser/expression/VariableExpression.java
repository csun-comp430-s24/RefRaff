package refraff.parser.expression;

public class VariableExpression extends Expression {

    private final String name; // I don't know if we need this
    
    public VariableExpression(String name) {
        super(name);
        this.name = name;
    }
}
