package refraff.parser.statement;

import refraff.parser.type.Type;
import refraff.parser.expression.VariableExpression;
import refraff.parser.expression.Expression;

public class Vardec extends Statement {
    
    private final Type type;
    private final VariableExpression variable; // I think I may move this into parser...
    private final Expression expression;

    public Vardec(Type type, 
            VariableExpression variable, Expression expression) {
        super(
            type.getParsedValue()
            + variable.getParsedValue()
            + expression.getParsedValue()
        );
        this.type = type;
        this.variable = variable;
        this.expression = expression;
    }
}
