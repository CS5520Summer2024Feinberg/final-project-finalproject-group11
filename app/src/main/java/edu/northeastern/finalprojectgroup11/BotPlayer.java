package edu.northeastern.finalprojectgroup11;

import java.util.Random;

public class BotPlayer {
    private GameBoard botBoard;
    private int boardRows;
    private int boardCols;
    private Random random;

    public BotPlayer(int boardRows, int boardCols) {
        this.boardRows = boardRows;
        this.boardCols = boardCols;
        this.botBoard = new GameBoard(boardRows, boardCols); // Initialize bot's board
        this.random = new Random();

        // Optionally, place mines on the bot's board
        placeMines();
    }

    public int[] attack(GameBoard playerBoard) {
        int row, col;
        do {
            row = random.nextInt(boardRows);
            col = random.nextInt(boardCols);
        } while (playerBoard.isCellClicked(row, col)); // Ensure the bot doesn't attack the same cell twice

        return new int[]{row, col}; // Return the chosen row and column
    }

    public GameBoard getBotBoard() {
        return botBoard;
    }

    private void placeMines() {
        // Randomly place mines on the bot's board, for example:
        int minesToPlace = 10; // Example: Place 10 mines
        while (minesToPlace > 0) {
            int row = random.nextInt(boardRows);
            int col = random.nextInt(boardCols);
            if (botBoard.placeMine(row, col) == 1) {
                minesToPlace--;
            }
        }
    }
}
