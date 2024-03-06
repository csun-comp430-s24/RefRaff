package refraff.parser.expression;

public class VariableExp extends Expression {

    private final String name; // I don't know if we need this
    
    public VariableExp(String name) {
        super(name);
        this.name = name;
    }
}
