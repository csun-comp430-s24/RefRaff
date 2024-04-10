package refraff.parser.function;

import java.util.List;
import java.util.Objects;

import refraff.parser.AbstractSyntaxTreeNode;
import refraff.parser.expression.Expression;

public class CommaExp extends AbstractSyntaxTreeNode {
    private static final String NODE_TYPE_DESCRIPTOR = "comma expressions";

    public final List<Expression> expressions;

    public CommaExp(List<Expression> expressions) {
        super(NODE_TYPE_DESCRIPTOR);

        this.expressions = expressions;
    }

    public List<Expression> getExpressions() {
        return this.expressions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), expressions);
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof CommaExp otherCommaExp
                && Objects.equals(expressions, otherCommaExp.expressions);
    }

}
