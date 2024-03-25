package refraff.parser.expression;

import refraff.parser.operator.*;

import java.util.Objects;

public class BinaryOpExp extends Expression {

    private static final String NODE_TYPE_DESCRIPTOR = "binary operator";

    private final Expression leftExp;
    private final OperatorEnum op;
    private final Expression rightExp;

    public BinaryOpExp(Expression leftExp, OperatorEnum op, Expression rightExp) {
        super(NODE_TYPE_DESCRIPTOR);

        this.leftExp = leftExp;
        this.op = op;
        this.rightExp = rightExp;
    }

    public Expression getLeftExp() {
        return leftExp;
    }

    public OperatorEnum getOp() {
        return op;
    }

    public Expression getRightExp() {
        return rightExp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getLeftExp(), getOp(), getRightExp());
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof BinaryOpExp binaryOpExp
                && Objects.equals(getLeftExp(), binaryOpExp.getLeftExp())
                && getOp() == binaryOpExp.getOp()
                && Objects.equals(getRightExp(), binaryOpExp.getRightExp());
    }

}
