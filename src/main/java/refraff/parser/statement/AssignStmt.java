package refraff.parser.statement;

import refraff.parser.Variable;
import refraff.parser.expression.Expression;

public class AssignStmt extends Statement {

    public final Variable variable; 
    public final Expression expression;

    public AssignStmt(Variable variable, Expression expression) {
        super(variable.hashCode() + expression.getParsedValue());
        this.variable = variable;
        this.expression = expression;
    }
}
