package refraff.parser.statement;

import refraff.parser.expression.Expression;

import java.util.Objects;
import java.util.Optional;

public class IfElseStmt extends Statement {

    private static final String NODE_TYPE_DESCRIPTOR = "if else";

    private final Expression condition;
    private final Statement ifBody;
    private final Optional<Statement> elseBody;

    public IfElseStmt(Expression condition, Statement ifBody) {
        this(condition, ifBody, null);
    }

    public IfElseStmt(Expression condition, Statement ifBody, Statement elseBody) {
        super(NODE_TYPE_DESCRIPTOR);

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
        return Objects.hash(super.hashCode(), condition, ifBody, elseBody);
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof IfElseStmt otherIfElse
                && Objects.equals(getCondition(), otherIfElse.getCondition())
                && Objects.equals(getIfBody(), otherIfElse.getIfBody())
                && Objects.equals(getElseBody(), otherIfElse.getElseBody());
    }

}
