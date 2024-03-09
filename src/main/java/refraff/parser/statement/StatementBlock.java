package refraff.parser.statement;

import java.util.List;

public class StatementBlock extends Statement {

    private final List<Statement> blockBody;

    public StatementBlock(List<Statement> blockBody) {
        super(String.valueOf(blockBody.hashCode()));

        this.blockBody = blockBody;
    }

    public List<Statement> getBlockBody() {
        return this.blockBody;
    }

    @Override
    public int hashCode() {
        return blockBody.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof StatementBlock otherBlock && blockBody.equals(otherBlock.getBlockBody());
    }

}
