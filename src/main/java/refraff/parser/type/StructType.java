package refraff.parser.type;

import refraff.parser.struct.StructName;

import java.util.Objects;
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
                && optionalStructName.equals(otherStructType.optionalStructName);
    }

    @Override
    public boolean hasTypeEquality(Type other) {
        if (!super.hasTypeEquality(other) || !(other instanceof StructType)) {
            return false;
        }

        StructType otherStructType = (StructType) other;
        if (isNullStruct() && otherStructType.isNullStruct()) {
            // If we're both null, what is our actual struct type? We have no idea
            return false;
        } else if (isNullStruct() == otherStructType.isNullStruct()) {
            // If we're both not null, check that our struct names match exactly
            return Objects.equals(getStructName().get(), otherStructType.getStructName().get());
        } else {
            // If one of us is null, then the null struct type will change its type to the type of our other struct
            return true;
        }
    }
}
