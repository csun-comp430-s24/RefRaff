package refraff.parser.statement;

import refraff.parser.type.Type;
import refraff.parser.Variable;
import refraff.parser.expression.primaryExpression.PrimaryExpression;

import java.util.Objects;

public class VardecStmt extends Statement {
    
    private final Type type;
    private final Variable variable; 
    private final PrimaryExpression expression;

    public VardecStmt(Type type, 
            Variable variable, PrimaryExpression expression) {
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

    public PrimaryExpression getExpression() {
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
