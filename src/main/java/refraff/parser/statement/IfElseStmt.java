package refraff.parser.statement;

import refraff.parser.expression.Expression;

import java.util.Objects;
import java.util.Optional;

public class IfElseStmt extends Statement {

    private final Expression condition;
    private final Statement ifBody;
    private final Optional<Statement> elseBody;

    public IfElseStmt(Expression condition, Statement ifBody) {
        this(condition, ifBody, null);
    }

    public IfElseStmt(Expression condition, Statement ifBody, Statement elseBody) {
        super("Unsure");

        this.condition = condition;
        this.ifBody = ifBody;
        this.elseBody = Optional.ofNullable(elseBody);
    }


    public Expression getCondition() {
        return condition;
    }

    public Statement getIfBody() {
        return ifBody;
    }

    public Optional<Statement> getElseBody() {
        return elseBody;
    }

    @Override
    public int hashCode() {
        return Objects.hash(condition, ifBody, elseBody);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof IfElseStmt otherIfElse && condition.equals(otherIfElse.condition)
                && ifBody.equals(otherIfElse.ifBody) && elseBody.equals(otherIfElse.elseBody);
    }

}
