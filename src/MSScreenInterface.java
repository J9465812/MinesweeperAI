import tileFunctions.BoardState;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.File;

public class MSScreenInterface {

    public static final int[][] topCornerMatrix = {{123, 123, 123, 123, 123}, {123, 123, 123, 123, 123}, {123, 123, 255, 255, 255}, {123, 123, 255, 255, 255}, {123, 123, 255, 255, 189}};
    public static final int[][] bottomCornerMatrix = {{123, 123, 255, 255, 189}, {123, 123, 255, 255, 189}, {255, 255, 255, 255, 189}, {255, 255, 255, 255, 189}, {189, 189, 189, 189, 189}};

    public boolean isValid;
    private final Robot robot;
    private Rectangle gameArea;
    private int width;
    private int height;
    private int totalBombs;
    private boolean isDead;

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTotalBombs() {
        return totalBombs;
    }

    public boolean isDead() {
        return isDead;
    }

    public MSScreenInterface() throws AWTException {

        robot = new Robot();
    }

    public void locateGame(){

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        BufferedImage img = robot.createScreenCapture(new Rectangle(0, 0, screenSize.width, screenSize.height));

        Point upperLeft = findPattern(img, topCornerMatrix);
        Point bottomRight = findPattern(img, bottomCornerMatrix);

        if(upperLeft == null || bottomRight == null){
            isValid = false;
            return;
        }

        gameArea = new Rectangle(upperLeft.x, upperLeft.y, bottomRight.x - upperLeft.x, bottomRight.y - upperLeft.y);

        width = gameArea.width/16;
        height = gameArea.height/16;

        totalBombs = readDisplay(img, new Point(upperLeft.x + 12, upperLeft.y - 27), 3);

        isValid = true;
    }

    private Point findPattern(BufferedImage img, int[][] pattern){

        int width = img.getWidth();
        int height = img.getHeight();
        int padding = pattern.length;

        BufferedImage b = new BufferedImage(width, height, 1);

        for(int x = 0; x < width - padding; x++){
            for(int y = 0; y < height - padding; y++){

                int delta = 0;

                for(int dx = 0; dx < padding; dx++){
                    for(int dy = 0; dy < padding; dy++){

                        int blue = img.getRGB(x + dx, y + dy) & 255;

                        delta += Math.abs(blue - pattern[dx][dy]);
                    }
                }

                if(delta == 0){
                    return new Point(x + padding/2, y + padding/2);
                }
            }
        }

        return null;
    }

    public static final int[] sevenSegmentDisplayCodes = {0b1011111, 0b0000101, 0b1110110, 0b1110101, 0b0101101, 0b1111001, 0b1111011, 0b1000101, 0b1111111, 0b1111101};
    public static final int[][] segmentOffsets = {{0, -9}, {0, 0}, {0, 9}, {-4, -5}, {4, -5}, {-4, 5}, {4, 5}};

    private int readDisplay(BufferedImage img, Point leftDigitPos, int numDigits){

        int total = 0;
        int x = leftDigitPos.x;
        int y = leftDigitPos.y;

        for(int digit = 0; digit < numDigits; digit++){

            total *= 10;

            int displayCode = 0;

            for(int n = 0; n < 7; n++){

                displayCode = displayCode << 1;

                int segmentX = x + segmentOffsets[n][0];
                int segmentY = y + segmentOffsets[n][1];

                displayCode += ((img.getRGB(segmentX, segmentY) & 0x00ff0000) >> 16) == 255 ? 1 : 0;
            }

            for(int n = 0; n < 10; n++){
                if(displayCode == sevenSegmentDisplayCodes[n]){
                    total += n;
                    break;
                }
            }

            x += 13;
        }

        return total;
    }

    public static int[] numberColors = {0xffbdbdbd, 0xff0000ff, 0xff007b00, 0xffff0000, 0xff00007b, 0xff7b0000, 0xff007b7b, 0xff000000};
    public BoardState readBoard(){


        BufferedImage b = robot.createScreenCapture(gameArea);

        BoardState.Tile[][] tiles = new BoardState.Tile[width][height];

        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){

                int cellCornerX = x * 16;
                int cellCornerY = y * 16;

                boolean isRevealed = b.getRGB(cellCornerX, cellCornerY) == 0xff7b7b7b;
                boolean isBomb = !isRevealed && b.getRGB(cellCornerX + 8, cellCornerY + 8) == 0xff000000;
                isDead |= b.getRGB(cellCornerX + 1, cellCornerY + 1) == 0xffff0000;

                int numberColor = b.getRGB(cellCornerX + 8, cellCornerY + 8);

                int number = -1;

                if(isRevealed){
                    for(int n = 0; n < numberColors.length; n++){
                        if(numberColor == numberColors[n]){
                            number = n;
                            break;
                        }
                    }
                }

                tiles[x][y] = new BoardState.Tile(isBomb, isRevealed, number);
            }
        }

        return new BoardState(tiles);
    }

    public void action(CellAction action){

        int pointX = gameArea.x + action.x * 16 + 8;
        int pointY = gameArea.y + action.y * 16 + 8;

        int buttonMask = action.type == CellAction.ActionType.MARK_BOMB ? InputEvent.BUTTON3_DOWN_MASK : InputEvent.BUTTON1_DOWN_MASK;

        robot.mouseMove(pointX, pointY);
        robot.mousePress(buttonMask);
        robot.mouseRelease(buttonMask);
    }

    public void restartGame(){

        robot.mouseMove(gameArea.x + gameArea.width / 2, gameArea.y - 27);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        isDead = false;
    }

    public static void main(String[] args) throws AWTException{
        MSScreenInterface i = new MSScreenInterface();

        i.readBoard();
    }
}
