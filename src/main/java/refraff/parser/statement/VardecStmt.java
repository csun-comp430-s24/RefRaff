package refraff.parser.statement;

import refraff.parser.type.Type;
import refraff.parser.Variable;
import refraff.parser.expression.Expression;

public class VardecStmt extends Statement {
    
    private final Type type;
    private final Variable variable; 
    private final Expression expression;

    public VardecStmt(Type type, 
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
