package refraff.parser.type;

public class BoolType extends Type {

    private static final String NODE_TYPE_DESCRIPTOR = "bool type";

    public BoolType() {
        super(NODE_TYPE_DESCRIPTOR);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BoolType;
    }

}
