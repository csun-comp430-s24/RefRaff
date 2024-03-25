package refraff.parser.struct;

import java.util.List;
import java.util.Objects;

import refraff.parser.AbstractSyntaxTreeNode;

public class StructDef extends AbstractSyntaxTreeNode {

    private static final String NODE_TYPE_DESCRIPTOR = "struct definition";

    // structdef ::= `struct` structname `{` (param `;`)* `}`
    private final StructName structName; 
    private final List<Param> params;

    public StructDef(final StructName structName, final List<Param> params) {
        super(NODE_TYPE_DESCRIPTOR);

        this.structName = structName;
        this.params = params;
    }

    public StructName getStructName() {
        return structName;
    }

    public List<Param> getParams() {
        return params;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getStructName(), getParams());
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof StructDef otherStructDef
                && Objects.equals(getStructName(), otherStructDef.getStructName())
                && Objects.equals(getParams(), otherStructDef.getParams());
    }

}
