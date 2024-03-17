package refraff.parser.expression.primaryExpression;

import refraff.parser.type.StructName;
import refraff.parser.struct.StructActualParams;


public class StructAllocExp extends PrimaryExpression {
        
    private static final String STRUCT_ALLOC_EXP = "struct allocation expression";

    private final StructName structName;
    private final StructActualParams params;

    public StructAllocExp(StructName structName, StructActualParams params) {
        super(STRUCT_ALLOC_EXP);
        this.structName = structName;
        this.params = params;
    }

    public StructName getStructName() {
        return structName;
    }

    public StructActualParams getParams() {
        return params;
    }

}
