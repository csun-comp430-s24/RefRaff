package refraff.parser.type;

import refraff.parser.struct.StructName;

import java.util.Optional;

public class StructType extends Type {

    private final Optional<StructName> optionalStructName;

    public StructType(StructName structName) {
        super(structName == null ? "null instance" : structName.getParsedValue());

        this.optionalStructName = Optional.ofNullable(structName);
    }

    public Optional<StructName> getStructName() {
        return optionalStructName;
    }

    public boolean isNullStruct() {
        return optionalStructName.isEmpty();
    }

    @Override
    public int hashCode() {
        return optionalStructName.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof StructType otherStructType &&
                // If I or the other are null instances, but not both of us, or if we match our raw types
                ((isNullStruct() ^ otherStructType.isNullStruct()) ||
                        optionalStructName.equals(otherStructType.optionalStructName));
    }

}
