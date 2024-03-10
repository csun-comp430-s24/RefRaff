package refraff.parser.statement;

import refraff.parser.type.Type;
import refraff.parser.Variable;
import refraff.parser.expression.Expression;

import java.util.Objects;

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

    public Type getType() {
        return type;
    }

    public Variable getVariable() {
        return variable;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, variable, expression);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof VardecStmt otherVardec;
    }

}
