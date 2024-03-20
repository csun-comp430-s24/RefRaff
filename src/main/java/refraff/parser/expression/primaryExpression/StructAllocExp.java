package refraff.parser.expression.primaryExpression;

import refraff.parser.type.StructType;
import refraff.parser.struct.StructActualParams;

import java.util.Objects;


public class StructAllocExp extends PrimaryExpression {
        
    private static final String STRUCT_ALLOC_EXP = "struct allocation";

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

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getStructType(), getParams());
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof StructAllocExp otherStructAllocExp
                && Objects.equals(getStructType(), otherStructAllocExp.getStructType())
                && Objects.equals(getParams(), otherStructAllocExp.getParams());
    }

}
