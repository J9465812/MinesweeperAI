import tileFunctions.BoardState;
import tileFunctions.TileMask;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MSAI {

    public static final int intialRegionSize = 5;
    public static final int maxRegionSize = 9;

    public static void main(String[] args){
        MSAI ai = new MSAI();

        GameResult result;

        ai.screenInterface.locateGame();

        do{

            ai.screenInterface.restartGame();

            result = ai.solvePuzzle();

        }while(result != GameResult.WIN);

        System.out.println("Done");
    }

    private MSScreenInterface screenInterface;
    private volatile BoardState currentBoardState;
    private volatile ConcurrentLinkedQueue<CellAction> actionsQueue;
    private volatile boolean isActive;

    public MSAI() {

        try {
            screenInterface = new MSScreenInterface();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public synchronized GameResult solvePuzzle(){

        screenInterface.locateGame();

        if(!screenInterface.isValid){

            isActive = false;
            return GameResult.INVALID;
        }

        isActive = true;

        currentBoardState = screenInterface.readBoard();

        int revealedTileCount = currentBoardState.getMask(t -> t.isRevealed).count();

        if(revealedTileCount == 0){
            screenInterface.action(new CellAction(screenInterface.getWidth()/2, screenInterface.getHeight()/2, CellAction.ActionType.REVEAL));

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            currentBoardState = screenInterface.readBoard();
        }

        actionsQueue = new ConcurrentLinkedQueue<>();

        new Thread(this::applyActions).start();

        while(true){

            int rs = intialRegionSize;
            int actions = 0;

            do{
                //System.out.print("Calculating Region Size: " + rs);
                actions = getActions(rs);
                //System.out.println(" " + actions + " Actions Calculated");

                if(rs >= maxRegionSize){

                    revealRandomTile();
                    break;
                }

                rs += 2;

            }while(actions == 0);

            //System.out.println();

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            currentBoardState = screenInterface.readBoard();

            if(screenInterface.isDead()){

                isActive = false;
                return GameResult.LOSS;
            }

            TileMask unknownTiles = currentBoardState.getMask(t -> !t.isBomb && !t.isRevealed);

            if(unknownTiles.count() == 0){

                isActive = false;
                return GameResult.WIN;
            }
        }
    }

    private int getActions(int regionSize){

        int[] arrayBS = {0};

        TileMask revealedTiles = currentBoardState.getMask(t -> t.isRevealed);
        TileMask flaggedTiles = currentBoardState.getMask(t -> t.isBomb);

        TileMask relevantTiles = TileMask.intersect(TileMask.invert(flaggedTiles), TileMask.getNeighbors(revealedTiles));

        relevantTiles.forEach((x, y) -> {

            int tileSafety = testSquare(x, y, regionSize, relevantTiles, currentBoardState);

            CellAction newAction = null;

            if(tileSafety == 1){
                //System.out.println("(" + x + "," + y + ") Safe");
                newAction = new CellAction(x, y, CellAction.ActionType.REVEAL);
            }else if(tileSafety == -1){
                //System.out.println("(" + x + "," + y + ") Bomb");
                newAction = new CellAction(x, y, CellAction.ActionType.MARK_BOMB);
            }

            if(newAction != null && !actionsQueue.contains(newAction)){
                actionsQueue.add(newAction);
                arrayBS[0]++;
            }
        });

        return arrayBS[0];
    }

    private void revealRandomTile(){

        TileMask revealedTiles = currentBoardState.getMask(t -> t.isRevealed);
        TileMask flaggedTiles = currentBoardState.getMask(t -> t.isBomb);

        TileMask relevantTiles = TileMask.intersect(TileMask.invert(flaggedTiles), TileMask.getNeighbors(revealedTiles));

        int x;
        int y;
        Random r = new Random();

        do{
            x = r.nextInt(screenInterface.getWidth());
            y = r.nextInt(screenInterface.getHeight());
        }while(!relevantTiles.tiles[x][y]);

        actionsQueue.add(new CellAction(x, y, CellAction.ActionType.REVEAL));
    }

    private void applyActions(){

        while(isActive){
            if(actionsQueue.size() > 0){
                screenInterface.action(actionsQueue.poll());
            }
        }
    }

    private int testSquare(int x, int y, int regionSize, TileMask relevantTiles, BoardState state){

        int bombOccurances = 0;
        int validPossibilities = 0;

        TileMask testRegion = TileMask.rectMask(screenInterface.getWidth(), screenInterface.getHeight(), x - regionSize/2, y - regionSize/2, x + regionSize/2 + 1, y + regionSize/2 + 1);
        TileMask checkRegion = TileMask.rectMask(screenInterface.getWidth(), screenInterface.getHeight(), x - regionSize/2 + 1, y - regionSize/2 + 1, x + regionSize/2, y + regionSize/2);

        TileMask possibleBombLocations = TileMask.intersect(testRegion, relevantTiles);
        int numberOfPossibilities = 1 << possibleBombLocations.count();

        if(numberOfPossibilities > 1000000){
            return 0;
        }

        for(int n = 0; n < numberOfPossibilities; n++){

            TileMask bombs = TileMask.bitSelect(possibleBombLocations, n);

            if(isValidRegion(bombs, checkRegion, state)){
                validPossibilities++;

                if(bombs.tiles[x][y]){
                    bombOccurances++;
                }
            }

            if(validPossibilities > 0 && bombOccurances != 0 && bombOccurances != validPossibilities){
                break;
            }
        }

        if(bombOccurances == 0){
            return 1;
        }else if(bombOccurances == validPossibilities){
            return -1;
        }else{
            return 0;
        }
    }

    public static final int[][] neighborhood = {{-1, -1}, {-1, 0}, {0, -1}, {1, -1}, {-1, 1}, {1, 0}, {0, 1}, {1, 1}};

    private boolean isValidRegion(TileMask bombs, TileMask region, BoardState state){

        final boolean[] isValid = new boolean[1];

        isValid[0] = true;

        region.forEach((x, y) -> {

            if(!isValid[0]) return;

            int requiredNumberOfBombs = state.getTileAt(x, y).numberShown;
            int totalBombs = 0;

            if(requiredNumberOfBombs == -1){
                return;
            }

            for(int n = 0; n < 8; n++){
                int tileX = x + neighborhood[n][0];
                int tileY = y + neighborhood[n][1];

                if(tileX < 0 || tileY < 0 || tileX >= screenInterface.getWidth() || tileY >= screenInterface.getHeight()){
                    continue;
                }

                if(bombs.tiles[tileX][tileY] || state.getTileAt(tileX, tileY).isBomb){
                    totalBombs++;
                    if(totalBombs > requiredNumberOfBombs){
                        isValid[0] = false;
                        return;
                    }
                }
            }

            if(totalBombs < requiredNumberOfBombs){
                isValid[0] = false;
            }
        });

        return isValid[0];
    }
}
