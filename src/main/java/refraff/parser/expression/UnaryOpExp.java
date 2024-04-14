package refraff.parser.expression;

import refraff.parser.operator.OperatorEnum;

import java.util.Objects;

public class UnaryOpExp extends Expression {

    private static final String NODE_TYPE_DESCRIPTOR = "unary operator";
    
    private final OperatorEnum op;
    private final Expression exp;

    public UnaryOpExp(OperatorEnum op, Expression exp) {
        super(NODE_TYPE_DESCRIPTOR);

        this.op = op;
        this.exp = exp;
    }

    public OperatorEnum getOp() {
        return op;
    }

    public Expression getExp() {
        return exp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getOp(), getExp());
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof UnaryOpExp otherUnaryOpExp
                && getOp() == otherUnaryOpExp.getOp()
                && Objects.equals(getExp(), otherUnaryOpExp.getExp());
    }

}
