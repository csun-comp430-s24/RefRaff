package refraff.parser.expression.primaryExpression;

import refraff.parser.Variable;

public class VariableExp extends PrimaryExpression {

    private final Variable var;
    
    public VariableExp(Variable var) {
        super(var.name);

        this.var = var;
    }

    public Variable getVar() {
        return var;
    }
}
