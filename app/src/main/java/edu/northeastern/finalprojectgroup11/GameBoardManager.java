package edu.northeastern.finalprojectgroup11;

public class GameBoardManager {
    private static GameBoard gameBoard;

    // Static method to get the GameBoard instance
    public static GameBoard getGameBoard() {
        return gameBoard;
    }

    // Static method to set the GameBoard instance
    public static void setGameBoard(GameBoard board) {
        gameBoard = board;
    }

    // Static method to reset the GameBoard instance
    public static void resetGameBoard(int rows, int cols) {
        gameBoard = new GameBoard(rows, cols);
    }

    // Static method to clear the GameBoard instance
    public static void clearGameBoard() {
        gameBoard = null;
    }

}
