package refraff.parser.expression.primaryExpression;

import refraff.parser.function.CommaExp;
import refraff.parser.function.FunctionName;

import java.util.Objects;

public class FuncCallExp extends PrimaryExpression {
        
    private static final String NODE_TYPE_DESCRIPTOR = "function call";

    private final FunctionName funcName;
    private final CommaExp commaExp;

    public FuncCallExp(FunctionName funcName, CommaExp commaExp) {
        super(NODE_TYPE_DESCRIPTOR);

        this.funcName = funcName;
        this.commaExp = commaExp;

    }

    public FunctionName getFuncName() {
        return funcName;
    }

    public CommaExp getCommaExp() {
        return commaExp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getFuncName(), getCommaExp());
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof FuncCallExp otherFuncCallExp
                && Objects.equals(getFuncName(), otherFuncCallExp.getFuncName())
                && Objects.equals(getCommaExp(), otherFuncCallExp.getCommaExp());
    }
}
