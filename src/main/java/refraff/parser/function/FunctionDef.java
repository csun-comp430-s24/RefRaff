package refraff.parser.function;

import refraff.parser.AbstractSyntaxTreeNode;
import refraff.parser.statement.StmtBlock;
import refraff.parser.struct.Param;
import refraff.parser.type.Type;

import java.util.List;

public class FunctionDef extends AbstractSyntaxTreeNode {

    private static final String TYPE_DESCRIPTOR = "function definition";

    /*
     fdef ::= `func` funcname `(` comma_param `)` `:` type
         `{` stmt* `}`
     */
    private final FunctionName functionName;
    private final Type returnType;
    private final List<Param> params;
    private final StmtBlock functionBody;

    public FunctionDef(final FunctionName functionName, final List<Param> params,
                       final Type returnType, final StmtBlock functionBody) {
        super(functionName.getParsedValue());

        this.functionName = functionName;
        this.returnType = returnType;
        this.params = params;
        this.functionBody = functionBody;
    }

    @Override
    public String getNodeTypeDescriptor() {
        return TYPE_DESCRIPTOR;
    }

    public FunctionName getFunctionName() {
        return functionName;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Param> getParams() {
        return params;
    }

    public StmtBlock getFunctionBody() {
        return functionBody;
    }

}
