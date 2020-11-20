import java.util.Objects;

public class CellAction {

    public int x;
    public int y;
    public ActionType type;

    public CellAction(int x, int y, ActionType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public static enum ActionType{
        MARK_BOMB,
        REVEAL;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CellAction that = (CellAction) o;
        return x == that.x &&
                y == that.y &&
                type == that.type;
    }
}
