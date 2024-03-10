package refraff.parser.expression;

import refraff.parser.operator.OperatorEnum;

public class DotExp extends Expression {
    
    private final Expression leftExp;
    private final Expression rightVar;

    public DotExp(Expression leftExp, Expression rightVar) {
        super(leftExp.getParsedValue() + rightVar.getParsedValue());
        this.leftExp = leftExp;
        this.rightVar = rightVar;
    }

    public Expression getLeftExp() {
        return leftExp;
    }

    public Expression getRightVar() {
        return rightVar;
    }
    
}
