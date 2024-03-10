package refraff.parser.type;


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

    // I just want my linter to be quiet
    public String getStructNameType() {
        return STRUCT_NAME_TYPE;
    }
}
