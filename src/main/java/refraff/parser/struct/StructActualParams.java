package refraff.parser.struct;

import java.util.List;

import refraff.parser.AbstractSyntaxTreeNode;

public class StructActualParams extends AbstractSyntaxTreeNode {
    
    private static final String TYPE = "parameters";
    private static final String TYPE_DESCRIPTOR = "struct actual parameters";

    public final List<StructActualParam> params;

    public StructActualParams(List<StructActualParam> params) {
        super(TYPE);
        this.params = params;
    }

    @Override
    public String getNodeTypeDescriptor() {
        return TYPE_DESCRIPTOR;
    }
}
