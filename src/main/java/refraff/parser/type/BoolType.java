package refraff.parser.type;

public class BoolType extends Type {

    private static final String BOOL_TYPE = "bool";

    public BoolType() {
        super(BOOL_TYPE);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BoolType;
    }

    @Override
    public int hashCode() {
        return BOOL_TYPE.hashCode();
    }
}
