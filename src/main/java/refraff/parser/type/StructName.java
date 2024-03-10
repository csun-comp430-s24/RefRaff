package refraff.parser.type;

import refraff.parser.Variable;

public class StructName extends Type {
    
    private static final String STRUCT_NAME_TYPE = "struct";

    // I don't really think this should be var - is a string fine? Yes!
    public final String structName;

    public StructName(final String structName) {
        super(structName);

        this.structName = structName;
    }

    @Override
    public String getParsedValue() {
        return this.structName;
    }
}
