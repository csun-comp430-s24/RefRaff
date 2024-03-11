package refraff.parser.expression.primaryExpression;

import refraff.parser.expression.Expression;


public class ParenExp extends PrimaryExpression {
    
    private static final String PAREN_EXP = "parenthesitized expression";

    private final Expression exp;

    public ParenExp(Expression exp) {
        super(PAREN_EXP);   // Should I stringify the expression or something?
        this.exp = exp;
    }

    public Expression getExp() {
        return exp;
    }

}
