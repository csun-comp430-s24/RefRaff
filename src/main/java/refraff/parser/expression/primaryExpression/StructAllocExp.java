package refraff.parser.expression.primaryExpression;

import refraff.parser.type.Type;
import refraff.parser.struct.StructActualParams;


public class StructAllocExp extends PrimaryExpression {
        
    private static final String STRUCT_ALLOC_EXP = "struct allocation expression";

    private final Type structName;
    private final StructActualParams params;

    public StructAllocExp(Type structName, StructActualParams params) {
        super(STRUCT_ALLOC_EXP);
        this.structName = structName;
        this.params = params;
    }

    public Type getStructName() {
        return structName;
    }

    public StructActualParams getParams() {
        return params;
    }

}
