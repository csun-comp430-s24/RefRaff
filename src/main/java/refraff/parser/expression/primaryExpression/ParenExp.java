package refraff.parser.expression.primaryExpression;

import refraff.parser.expression.Expression;

import java.util.Objects;


public class ParenExp extends PrimaryExpression {
    
    private static final String NODE_TYPE_DESCRIPTOR = "parenthesized";

    private final Expression exp;

    public ParenExp(Expression exp) {
        super(NODE_TYPE_DESCRIPTOR);

        this.exp = exp;
    }

    public Expression getExp() {
        return exp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getExp());
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof ParenExp otherParenExp
                && Objects.equals(getExp(), otherParenExp.getExp());
    }

}
