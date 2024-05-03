package refraff.parser.struct;

import refraff.parser.AbstractSyntaxTreeNode;
import refraff.parser.expression.Expression;
import refraff.parser.Variable;

import java.util.Objects;

public class StructActualParam extends AbstractSyntaxTreeNode {

    private static final String NODE_TYPE_DESCRIPTOR = "struct actual parameter";

    public final Variable var;
    public final Expression exp;

    public StructActualParam(Variable var, Expression exp) {
        super(NODE_TYPE_DESCRIPTOR);

        this.var = var;
        this.exp = exp;
    }

    public Variable getVariable() {
        return var;
    }

    public Expression getExpression() {
        return exp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), var, exp);
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof StructActualParam otherStructActualParam
                && Objects.equals(var, otherStructActualParam.var)
                && Objects.equals(exp, otherStructActualParam.exp);
    }

}
