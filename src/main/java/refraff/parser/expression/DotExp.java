package refraff.parser.expression;

import refraff.parser.Variable;

// Should this be a regular binary operator? I feel weird putting any primary
// expression in rightVar, but I guess that's the type checker's problem?
public class DotExp extends Expression {
    
    private final Expression leftExp;
    private final Variable rightVar;

    public DotExp(Expression leftExp, Variable rightVar) {
        super(leftExp.getParsedValue() + rightVar.name);
        this.leftExp = leftExp;
        this.rightVar = rightVar;
    }

    public Expression getLeftExp() {
        return leftExp;
    }

    public Variable getRightVar() {
        return rightVar;
    }
    
}
