package refraff.parser.statement;

import java.util.List;
import java.util.Objects;

public class StmtBlock extends Statement {

    private static final String NODE_TYPE_DESCRIPTOR = "block";

    private final List<Statement> blockBody;

    public StmtBlock() {
        this(List.of());
    }

    public StmtBlock(List<Statement> blockBody) {
        super(NODE_TYPE_DESCRIPTOR);

        this.blockBody = blockBody;
    }

    public List<Statement> getBlockBody() {
        return this.blockBody;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), blockBody);
    }

    @Override
    public boolean equals(Object other) {
        return super.equals(other)
                && other instanceof StmtBlock otherBlock
                && Objects.equals(getBlockBody(), otherBlock.getBlockBody());
    }

}
