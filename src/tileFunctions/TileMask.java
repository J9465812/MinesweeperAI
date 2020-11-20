package tileFunctions;

import java.awt.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public class TileMask {

    public final boolean[][] tiles;

    public TileMask(boolean[][] tiles) {
        this.tiles = tiles;
    }

    public TileMask(int width, int height, BiPredicate<Integer, Integer> condition){

        tiles = new boolean[width][height];

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){

                tiles[x][y] = condition.test(x, y);
            }
        }
    }

    public void forEach(BiConsumer<Integer, Integer> function){

        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                if(tiles[x][y])
                    function.accept(x, y);
            }
        }
    }

    public int count(){
        int total = 0;
        for(int x = 0; x < tiles.length; x++){
            for(int y = 0; y < tiles[0].length; y++){
                if(tiles[x][y]) total ++;
            }
        }
        return total;
    }

    public static TileMask intersect(TileMask a, TileMask b){
        return functionMerge(a, b, (x, y) -> x && y);
    }

    public static TileMask union(TileMask a, TileMask b){
        return functionMerge(a, b, (x, y) -> x || y);
    }

    public static TileMask invert(TileMask a){
        return functionMerge(a, a, (x, y) -> !x);
    }

    public static TileMask functionMerge(final TileMask a, final TileMask b, BiFunction<Boolean, Boolean, Boolean> function){

        if(a.tiles.length != b.tiles.length || a.tiles[0].length != b.tiles[0].length){
            throw new IllegalArgumentException("Merging TileMasks must be of the same size.");
        }

        return new TileMask(a.tiles.length, a.tiles[0].length, (x, y) -> function.apply(a.tiles[x][y], b.tiles[x][y]));
    }

    public static TileMask rectMask(int width, int height, int xMin, int yMin, int xMax, int yMax){
        return new TileMask(width, height, (x, y) -> (x >= xMin && x < xMax && y >= yMin && y < yMax));
    }

    public static TileMask getNeighbors(final TileMask a){

        return TileMask.intersect(TileMask.invert(a), new TileMask(a.tiles.length, a.tiles[0].length, (x, y) -> TileMask.intersect(a, TileMask.rectMask(a.tiles.length, a.tiles[0].length, x - 1, y - 1, x + 2, y + 2)).count() != 0));
    }

    public static TileMask bitSelect(TileMask a, long bits){

        int bitMask = 1;

        boolean[][] output = new boolean[a.tiles.length][a.tiles[0].length];

        for(int x = 0; x < a.tiles.length; x++){
            for(int y = 0; y < a.tiles[0].length; y++){

                if(a.tiles[x][y]){
                    output[x][y] = (bits & bitMask) > 0;
                    bitMask = bitMask << 1;
                }
            }
        }

        return new TileMask(output);
    }
}
