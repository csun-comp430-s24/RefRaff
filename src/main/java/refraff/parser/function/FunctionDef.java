package refraff.parser.function;

import refraff.parser.AbstractSyntaxTreeNode;
import refraff.parser.statement.StmtBlock;
import refraff.parser.struct.Param;
import refraff.parser.type.Type;

import java.util.List;
import java.util.Objects;

public class FunctionDef extends AbstractSyntaxTreeNode {

    private static final String NODE_TYPE_DESCRIPTOR = "function definition";

    /*
     fdef ::= `func` funcname `(` comma_param `)` `:` type
         `{` stmt* `}`
     */
    private final FunctionName functionName;
    private final List<Param> params;
    private final Type returnType;
    private final StmtBlock functionBody;

    public FunctionDef(final FunctionName functionName, final List<Param> params,
                       final Type returnType, final StmtBlock functionBody) {
        super(NODE_TYPE_DESCRIPTOR);

        this.functionName = functionName;
        this.params = params;
        this.returnType = returnType;
        this.functionBody = functionBody;

        // Also create a function signature for the function map in the typechecker
    }

    public FunctionName getFunctionName() {
        return functionName;
    }

    public List<Param> getParams() {
        return params;
    }

    public Type getReturnType() {
        return returnType;
    }

    public StmtBlock getFunctionBody() {
        return functionBody;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getFunctionName(), getParams(), getReturnType(), getFunctionBody());
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof FunctionDef otherFunctionDef
                && Objects.equals(getFunctionName(), otherFunctionDef.getFunctionName())
                && Objects.equals(getParams(), otherFunctionDef.getParams())
                && Objects.equals(getReturnType(), otherFunctionDef.getReturnType())
                && Objects.equals(getFunctionBody(), otherFunctionDef.getFunctionBody());
    }

}
