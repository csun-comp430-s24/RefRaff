package refraff.parser;

import refraff.parser.statement.Statement;
import refraff.parser.statement.StatementBlock;
import refraff.parser.type.FunctionName;
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
    private final StatementBlock functionBody;

    public FunctionDef(final FunctionName functionName, final List<Param> params,
                       final Type returnType, final StatementBlock functionBody) {
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

    public StatementBlock getFunctionBody() {
        return functionBody;
    }

}
