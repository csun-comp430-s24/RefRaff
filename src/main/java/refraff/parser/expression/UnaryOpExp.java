package refraff.parser.expression;

import refraff.parser.operator.OperatorEnum;

public class UnaryOpExp extends Expression {
    
    private final OperatorEnum op;
    private final Expression exp;

    public UnaryOpExp(OperatorEnum op, Expression exp) {
        super(op.getSymbol() + exp.getParsedValue());
        this.op = op;
        this.exp = exp;
    }

    public OperatorEnum getOp() {
        return op;
    }

    public Expression getExp() {
        return exp;
    }
    
}
