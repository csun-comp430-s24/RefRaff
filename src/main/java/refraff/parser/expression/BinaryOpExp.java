package refraff.parser.expression;

import refraff.parser.operator.*;

public class BinaryOpExp extends Expression {

    private final Expression leftExp;
    private final Operator op;
    private final Expression rightExp;

    public BinaryOpExp(Expression leftExp, Operator op, Expression rightExp) {
        super(leftExp.getParsedValue() + op.getParsedValue() + rightExp.getParsedValue());
        this.leftExp = leftExp;
        this.op = op;
        this.rightExp = rightExp;
    }
}
