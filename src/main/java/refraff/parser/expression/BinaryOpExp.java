package refraff.parser.expression;

import refraff.parser.operator.*;

public class BinaryOpExp extends Expression {

    private final Expression leftExp;
    private final OperatorEnum op;
    private final Expression rightExp;

    public BinaryOpExp(Expression leftExp, OperatorEnum op, Expression rightExp) {
        super(leftExp.getParsedValue() + op.getSymbol() + rightExp.getParsedValue());
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
    
}
