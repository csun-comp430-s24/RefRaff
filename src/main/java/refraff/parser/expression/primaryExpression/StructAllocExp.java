package refraff.parser.expression.primaryExpression;

import refraff.parser.type.StructType;
import refraff.parser.struct.StructActualParams;


public class StructAllocExp extends PrimaryExpression {
        
    private static final String STRUCT_ALLOC_EXP = "struct allocation expression";

    private final StructType structType;
    private final StructActualParams params;

    public StructAllocExp(StructType structType, StructActualParams params) {
        super(STRUCT_ALLOC_EXP);

        this.structType = structType;
        this.params = params;
    }

    public StructType getStructType() {
        return structType;
    }

    public StructActualParams getParams() {
        return params;
    }

}
