package refraff.parser.statement;

import refraff.parser.type.Type;
import refraff.parser.Variable;
import refraff.parser.expression.Expression;

public class Vardec extends Statement {
    
    private final Type type;
    private final Variable variable; // I think I may move this into parser...
    private final Expression expression;

    public Vardec(Type type, 
            Variable variable, Expression expression) {
        super(
            type.getParsedValue()
            + variable.hashCode()
            + expression.getParsedValue()
        );
        this.type = type;
        this.variable = variable;
        this.expression = expression;
    }
}
