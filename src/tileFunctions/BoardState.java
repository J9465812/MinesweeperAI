package tileFunctions;

import java.util.function.Predicate;

public class BoardState {

    public static class Tile {

        public final boolean isBomb;
        public final boolean isRevealed;
        public final int numberShown;

        public Tile(boolean isBomb, boolean isRevealed, int numberShown) {
            this.isBomb = isBomb;
            this.isRevealed = isRevealed;
            this.numberShown = numberShown;
        }
    }

    private Tile[][] board;

    public Tile getTileAt(int x, int y){
        return board[x][y];
    }

    public BoardState(Tile[][] board) {
        this.board = board;
    }

    public TileMask getMask(Predicate<Tile> condition){

        boolean[][] mask = new boolean[board.length][board[0].length];

        for(int x = 0; x < mask.length; x++){
            for(int y = 0; y < mask[0].length; y++){

                mask[x][y] = condition.test(board[x][y]);
            }
        }

        return new TileMask(mask);
    }
}
