package refraff.parser.type;

import refraff.parser.Variable;

public class StructName extends Type {
    
    private static final String STRUCT_NAME_TYPE = "struct";

    // I don't really think this should be var - is a string fine? idk
    public final Variable var;

    public StructName(final Variable var) {
        super(var.name);
        this.var = var;
    }

    @Override
    public String getParsedValue() {
        return var.name;
    }
}
