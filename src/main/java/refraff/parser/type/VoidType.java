package refraff.parser.type;

public class VoidType extends Type {

    private static final String VOID_TYPE = "void";

    public VoidType() {
        super(VOID_TYPE);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof VoidType;
    }

    @Override
    public int hashCode() {
        return VOID_TYPE.hashCode();
    }
}
