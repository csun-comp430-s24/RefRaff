package refraff.parser.struct;

import java.util.List;
import java.util.Objects;

import refraff.parser.AbstractSyntaxTreeNode;

public class StructActualParams extends AbstractSyntaxTreeNode {

    private static final String NODE_TYPE_DESCRIPTOR = "struct actual parameters";

    public final List<StructActualParam> params;

    public StructActualParams(List<StructActualParam> params) {
        super(NODE_TYPE_DESCRIPTOR);

        this.params = params;
    }

    public List<StructActualParam> getStructActualParams() {
        return this.params;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), params);
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof StructActualParams otherStructActualParams
                && Objects.equals(params, otherStructActualParams.params);
    }

}
