package refraff.parser.statement;

import refraff.parser.Variable;
import refraff.parser.expression.Expression;

import java.util.Objects;

public class AssignStmt extends Statement {

    private static final String NODE_TYPE_DESCRIPTOR = "assignment";

    public final Variable variable; 
    public final Expression expression;

    public AssignStmt(Variable variable, Expression expression) {
        super(NODE_TYPE_DESCRIPTOR);

        this.variable = variable;
        this.expression = expression;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), variable, expression);
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof AssignStmt otherAssignStmt
                && Objects.equals(variable, otherAssignStmt.variable)
                && Objects.equals(expression, otherAssignStmt.expression);
    }

}
