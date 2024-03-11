package refraff.parser.statement;

import refraff.parser.expression.Expression;

import java.util.Objects;

public class WhileStmt extends Statement {

    private final Expression condition;
    private final Statement body;

    public WhileStmt(Expression condition, Statement body) {
        super(condition.getParsedValue() + body.getParsedValue());

        this.condition = condition;
        this.body = body;
    }

    public Expression getCondition() {
        return condition;
    }

    public Statement getBody() {
        return body;
    }

    @Override
    public int hashCode() {
        return Objects.hash(condition, body);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof WhileStmt otherWhile && condition.equals(otherWhile.condition)
                && body.equals(otherWhile.body);
    }

}
