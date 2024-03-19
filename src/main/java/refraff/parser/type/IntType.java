package refraff.parser.type;

public class IntType extends Type {
    
    private static final String INT_TYPE = "int";

    public IntType() {
        super(INT_TYPE);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof IntType;
    }

    @Override
    public int hashCode() {
        return INT_TYPE.hashCode();
    }
}
