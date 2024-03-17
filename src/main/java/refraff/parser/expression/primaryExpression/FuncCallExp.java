package refraff.parser.expression.primaryExpression;

import refraff.parser.function.CommaExp;
import refraff.parser.function.FunctionName;

public class FuncCallExp extends PrimaryExpression {
        
    private static final String FUNC_CALL_EXP = "function call expression";

    private final FunctionName funcName;
    private final CommaExp commaExp;

    public FuncCallExp(FunctionName funcName, CommaExp commaExp) {
        super(FUNC_CALL_EXP);   // Should I stringify the expression or something?
        this.funcName = funcName;
        this.commaExp = commaExp;

    }

    public FunctionName getFuncName() {
        return funcName;
    }

    public CommaExp getCommaExp() {
        return commaExp;
    }

}
