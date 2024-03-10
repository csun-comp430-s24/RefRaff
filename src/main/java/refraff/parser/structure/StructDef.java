package refraff.parser.structure;

import java.util.List;

import refraff.parser.AbstractSyntaxTreeNode;
import refraff.parser.type.StructName;

public class StructDef extends AbstractSyntaxTreeNode {

    private static final String TYPE_DESCRIPTOR = "StructDef";

    // structdef ::= `struct` structname `{` (param `;`)* `}`
    private final StructName structName; 
    private final List<Param> params;

    public StructDef(final StructName structName, final List<Param> params) {
        // This looks pretty bad, but I'm not sure how to handle it
        super(structName.getParsedValue());

        this.structName = structName;
        this.params = params;
    }

    @Override
    public String getNodeTypeDescriptor() {
        return TYPE_DESCRIPTOR;
    }

    public StructName getStructName() {
        return structName;
    }

    public List<Param> getParams() {
        return params;
    }

}
