package refraff;

import java.util.Objects;

public class SourcePosition {

    public static final int STARTING_LINE_POSITION = 1;
    public static final int STARTING_COLUMN_POSITION = 1;

    public static final SourcePosition DEFAULT_SOURCE_POSITION = new SourcePosition(
            STARTING_LINE_POSITION,
            STARTING_COLUMN_POSITION
    );

    private final int linePosition;
    private final int columnPosition;

    public SourcePosition(int linePosition, int columnPosition) {
        this.linePosition = linePosition;
        this.columnPosition = columnPosition;
    }

    public int getLinePosition() {
        return linePosition;
    }

    public int getColumnPosition() {
        return columnPosition;
    }

    public SourcePosition getSpaceSeparators(SourcePosition other) {
        if (getLinePosition() == other.getLinePosition()) {
            return new SourcePosition(0, Math.abs(getColumnPosition() - other.getColumnPosition()));
        }

        // We start counting at 1, so take away that first "space" that should be added
        int columnPosition = getLinePosition() > other.getLinePosition() ? getColumnPosition() : other.getColumnPosition();
        columnPosition -= 1;

        return new SourcePosition(Math.abs(getLinePosition() - other.getLinePosition()), columnPosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLinePosition(), getColumnPosition());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SourcePosition otherPosition
                && getLinePosition() == otherPosition.getLinePosition()
                && getColumnPosition() == otherPosition.getColumnPosition();
    }

    public String toLinePosition(boolean isEndPosition) {
        int columnPosition = isEndPosition ? getColumnPosition() - 1 : getColumnPosition();
        return String.format("line %d, column %d", getLinePosition(), columnPosition);
    }

    @Override
    public String toString() {
        return toLinePosition(false);
    }

}
