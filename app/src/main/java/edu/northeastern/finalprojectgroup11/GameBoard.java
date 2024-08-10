package edu.northeastern.finalprojectgroup11;

import java.util.ArrayList;
import java.util.List;

public class GameBoard {
    private int rows;
    private int cols;
    private int mineToPlace = 10;
    private boolean [][] mineLeft;
    private boolean [][] isClicked;
    private boolean [][] mineTotal; // mine in here don't get remove
    private int mineLeftCount = 10;

    public GameBoard(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.mineLeft = new boolean[rows][cols];
        this.isClicked = new boolean[rows][cols];
        this.mineTotal = new boolean[rows][cols];
    }

    public int getRows() {
        return this.rows;
    }

    public int getCols() {
        return this.cols;
    }

    public int getMineToPlace() {
        return this.mineToPlace;
    }

    // 1 indicate mine place success, 0 is already have mine, -1 is out of board
    public int placeMine(int row, int col) {
        if (row >= this.rows || col >= this.cols || row < 0 || col < 0) {
            return -1;
        } else if (this.mineLeft[row][col]) {
            return 0;
        } else {
            this.mineLeft[row][col] = true;
            this.mineTotal[row][col] = true;
            mineToPlace--;
            return 1;
        }
    }

    public int removeMineDeploy(int row, int col) {
        if (row >= this.rows || col >= this.cols || row < 0 || col < 0) {
            return -1;
        } else if (!this.mineLeft[row][col]) {
            return 0;
        } else {
            this.mineLeft[row][col] = false;
            this.mineTotal[row][col] = false;
            mineToPlace++;
            return 1;
        }
    }

    public boolean isDeployReady() {
        return this.mineToPlace == 0;
    }

    public int removeMine(int row, int col) {
        if (row >= this.rows || col >= this.cols || row < 0 || col < 0 && mineLeftCount > 0) {
            return -1;
        } else if (!this.mineLeft[row][col]) {
            return 0;
        } else {
            this.mineLeft[row][col] = false;
            mineLeftCount--;
            return 1;
        }
    }

    public boolean hasMine(int row, int col) {
        if (row >= this.rows || col >= this.cols || row < 0 || col < 0) {
            return false;
        } else {
            return this.mineLeft[row][col];
        }
    }

    public int countMindAroundLeft(int row, int col) {
        if (row >= this.rows || col >= this.cols || row < 0 || col < 0) {
            return -1;
        }
        int count = 0;
        for (int i = row - 1; i <= row + 1; i++) {
            for (int j = col - 1; j <= col +1; j++) {
                // only count within the board
                if (i < this.rows && j < this.cols && i >= 0 && j >= 0 && !(i == row && j == col)) {
                    if (mineLeft[i][j]) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public int countMindAroundTotal(int row, int col) {
        if (row >= this.rows || col >= this.cols || row < 0 || col < 0) {
            return -1;
        }
        int count = 0;
        for (int i = row - 1; i <= row + 1; i++) {
            for (int j = col - 1; j <= col +1; j++) {
                // only count within the board
                if (i < this.rows && j < this.cols && i >= 0 && j >= 0 && !(i == row && j == col)) {
                    if (mineTotal[i][j]) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    // keep track of what cell is clicked for auto surrender mine check
    public void clickCell(int row, int col) {
        isClicked[row][col] = true;
    }

    public boolean isCellClicked(int row, int col) {
        return isClicked[row][col];
    }

    public boolean hadMine(int row, int col) {
        if (row >= this.rows || col >= this.cols || row < 0 || col < 0) {
            return false;
        } else {
            return this.mineTotal[row][col];
        }
    }

    // Clear all the mine on the board
    public void clearMines() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (hasMine(i, j)) {
                    removeMineDeploy(i, j);
                }
            }
        }
    }

    public int getMineLeftCount() {
        return mineLeftCount;
    }

    public boolean isAllFound() {
        return mineLeftCount == 0;
    }
    public List<String> getAllMineLocations() {
        List<String> mines = new ArrayList<>();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (mineTotal[row][col]) {
                    mines.add(row + "," + col);
                }
            }
        }
        return mines;
    }

}
