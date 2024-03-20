package refraff.parser.type;

public class VoidType extends Type {

    private static final String NODE_TYPE_DESCRIPTOR = "void type";

    public VoidType() {
        super(NODE_TYPE_DESCRIPTOR);
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
