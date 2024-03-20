package refraff.parser.type;

import refraff.parser.struct.StructName;

import java.util.Optional;

public class StructType extends Type {

    private static final String NODE_TYPE_DESCRIPTOR = "struct type";

    private final Optional<StructName> optionalStructName;

    public StructType(StructName structName) {
        super(NODE_TYPE_DESCRIPTOR);

        this.optionalStructName = Optional.ofNullable(structName);
    }

    public Optional<StructName> getStructName() {
        return optionalStructName;
    }

    public boolean isNullStruct() {
        return optionalStructName.isEmpty();
    }

    @Override
    public boolean shouldThrowOnAssignment() {
        return false;
    }

    @Override
    public int hashCode() {
        return optionalStructName.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof StructType otherStructType
                // If I or the other are null instances, but not both of us, or if we match our raw types
                && ((isNullStruct() ^ otherStructType.isNullStruct()) ||
                        optionalStructName.equals(otherStructType.optionalStructName));
    }

}
