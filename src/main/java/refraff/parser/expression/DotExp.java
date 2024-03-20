package refraff.parser.expression;

import refraff.parser.Variable;

import java.util.Objects;

// Should this be a regular binary operator? I feel weird putting any primary
// expression in rightVar, but I guess that's the type checker's problem?

// Since dot_exp ::= primary_exp (`.` var)*, I think it's good to have it separate from other bin-ops, this is more
// restrictive than bin ops that accept any expressions as arguments
public class DotExp extends Expression {

    private static final String NODE_TYPE_DESCRIPTOR = "dot";
    
    private final Expression leftExp;
    private final Variable rightVar;

    public DotExp(Expression leftExp, Variable rightVar) {
        super(NODE_TYPE_DESCRIPTOR);

        this.leftExp = leftExp;
        this.rightVar = rightVar;
    }

    public Expression getLeftExp() {
        return leftExp;
    }

    public Variable getRightVar() {
        return rightVar;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getLeftExp(), getRightVar());
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof DotExp otherDotExp
                && Objects.equals(getLeftExp(), otherDotExp.getLeftExp())
                && Objects.equals(getRightVar(), otherDotExp.getRightVar());
    }

}
