package refraff.parser.type;

public class IntType extends Type {

    private static final String NODE_TYPE_DESCRIPTOR = "int type";

    public IntType() {
        super(NODE_TYPE_DESCRIPTOR);
    }

    @Override
    public String toString() {
        return "int";
    }
}
