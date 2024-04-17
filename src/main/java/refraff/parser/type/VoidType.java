package refraff.parser.type;

public class VoidType extends Type {

    private static final String NODE_TYPE_DESCRIPTOR = "void type";

    public VoidType() {
        super(NODE_TYPE_DESCRIPTOR);
    }

    @Override
    public String toString() {
        return "void";
    }
}
