package edu.northeastern.finalprojectgroup11;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.os.CountDownTimer;
import android.widget.Toast;

import androidx.gridlayout.widget.GridLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;


// TODO: quit button, timer also count for bot, finsih acticity from deploy, step left, winner checking
// icon for mine, misses(step left) on both side, timer, dialog pop up, link quit with back
public class BotBattleActivity extends AppCompatActivity {
    private final int boardRows = 10;
    private final int boardCols = 10;
    private GameBoard myBoard;
    private GridLayout myGridLayout;
    private BotPlayer bot;
    private GameBoard botBoard;
    private GridLayout botGridLayout;
    private Handler handler; // Handler for managing delays
    private boolean myTurn = true;

    private CountDownTimer countDownTimer;
    private TextView countdownTextView; // TextView to show the countdown

    private int selectedRow = -1;
    private int selectedCol = -1;
    private Button lastSelectedButton = null; // To keep track of the last selected button
    String selectTextHolder; // hold the stuff on the cell


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bot_battle);
        handler = new Handler(Looper.getMainLooper());
        countdownTextView = findViewById(R.id.countdownTextView); // Assuming you have this in your layout

        // Initialize the countdown timer for 10 seconds
        countDownTimer = new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
                // Update the countdown text each second
                countdownTextView.setText("" + millisUntilFinished / 1000);
            }

            public void onFinish() {
                // Automatically click button if run out time
                if (lastSelectedButton != null) {
                    launchConfirm();
                } else {
                    // Automatically click a random block if time runs out and not select block
                    Random random = new Random();
                    int row, col;
                    do {
                        row = random.nextInt(boardRows);
                        col = random.nextInt(boardCols);
                    } while (botBoard.isCellClicked(row, col)); // Ensure doesn't attack the same cell twice

                    onCellClick(row, col); // Simulate the click
                    launchConfirm();
                    Toast.makeText(BotBattleActivity.this, "Time's up! Random move selected.", Toast.LENGTH_SHORT).show();
                }
            }
        };

        // Start the countdown when the activity is created
        countDownTimer.start();

        // Big Board
        // Set up Bot's board
        bot = new BotPlayer(boardRows, boardCols);
        botBoard = bot.getBotBoard();

        // Set the hit button, after hit ai attack
        Button btnLaunch = findViewById(R.id.buttonLaunch);

        // Draw the game board
        botGridLayout = findViewById(R.id.gridLayoutMinePlacement);
        int totalWidth = botGridLayout.getWidth();
        int buttonSize = totalWidth / boardCols;
        int remainder = totalWidth % boardCols; // Calculate any remainder pixels
        for (int i = 0; i < boardRows; i++) {
            for (int j = 0; j < boardCols; j++) {
                Button button = new Button(this);
                
                // Use drawable
                button.setBackgroundResource(R.drawable.button_border);
                
                // set up button and size
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(i, 1f), GridLayout.spec(j, 1f));
                params.width = buttonSize + (j < remainder ? 1 : 0); // Distribute the remainder pixels
                params.height = params.width;
                button.setLayoutParams(params);
                
                // Show mine just for test
                if (botBoard.hasMine(i, j)) {
                    button.setText("M");
                }

                // Set color based on checkerboard pattern
                if ((i + j) % 2 == 0) {
                    button.setBackgroundColor(Color.parseColor("#2980fb"));
                } else {
                    button.setBackgroundColor(Color.parseColor("#97C0FB"));
                }

                botGridLayout.addView(button);

                // Set click action
                final int row = i;
                final int col = j;
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (myTurn) {
                            onCellClick(row, col);
                        }
                    }
                });
            }
        }


        // Small payer board
        // Set up player board, retrieve from static class
        myBoard = GameBoardManager.getGameBoard();
        GameBoardManager.clearGameBoard(); // clear manager after retrieve board


        // Draw the game board
        myGridLayout = findViewById(R.id.gridLayoutMyMine);
        totalWidth = botGridLayout.getWidth();
        buttonSize = totalWidth / boardCols;
        remainder = totalWidth % boardCols; // Calculate any remainder pixels
        for (int i = 0; i < boardRows; i++) {
            for (int j = 0; j < boardCols; j++) {
                Button button = new Button(this);

                // Use drawable
                button.setBackgroundResource(R.drawable.button_border);

                // set up button and size
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(i, 1f), GridLayout.spec(j, 1f));
                params.width = buttonSize + (j < remainder ? 1 : 0); // Distribute the remainder pixels
                params.height = params.width;
                button.setLayoutParams(params);

                // Show mine on your own board
                if (myBoard.hasMine(i, j)) {
                    button.setText("\u26AB");
                    button.setGravity(Gravity.CENTER);
                    button.setTextSize(10);
                    button.setPadding(5, 5, 5, 5);
                }

                // Set color based on checkerboard pattern
                if ((i + j) % 2 == 0) {
                    button.setBackgroundColor(Color.parseColor("#2980fb"));
                } else {
                    button.setBackgroundColor(Color.parseColor("#97C0FB"));
                }

                myGridLayout.addView(button);

                // Set click action
                final int row = i;
                final int col = j;
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onCellConfirm(row, col, myBoard, myGridLayout, 10, true);
                    }
                });
            }
        }

        // Button for player to launch
        btnLaunch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchConfirm();
            }
        });


    }

    // Cell is selected on opponent board
    private void onCellClick(int row, int col) {
        Button button = (Button) botGridLayout.getChildAt(row * boardCols + col);

        // Clear the outline from the last selected button
        if (lastSelectedButton != null) {
            lastSelectedButton.setText(selectTextHolder); // rest the last button
        }

        // Mark this button as selected
        selectTextHolder = button.getText().toString();
        button.setText("\u274C"); // this is a cross
        button.setTextColor(Color.parseColor("#E6301F"));
        button.setGravity(Gravity.CENTER);
        button.setTextSize(20);
        button.setPadding(5, 5, 5, 5);
        selectedRow = row;
        selectedCol = col;
        lastSelectedButton = button; // Keep track of this button
    }

    // Action when launch is clicked
    private void onCellConfirm(int row, int col, GameBoard board, GridLayout gridLayout, int textSize, boolean player) {  // 0 bot, 1 player
        Button button = (Button) gridLayout.getChildAt(row * boardCols + col);
        button.setTextColor(Color.WHITE);
        // The mine is found
        if (board.hasMine(row, col)) {
            button.setEnabled(false);
            // set mine as text
            button.setText("\u26AB");
            button.setGravity(Gravity.CENTER);
            button.setTextSize(textSize);
            button.setPadding(5, 5, 5, 5);
            if (player) { // player get hit is red, bot get hit is green
                button.setBackgroundColor(Color.parseColor("#E6301F"));
            } else {
                button.setBackgroundColor(Color.parseColor("#29E67D"));
            }

            // perform re-click to show area without mine
            board.removeMine(row, col);
            for (int i=0; i < boardRows; i++) {
                for (int j=0; j < boardCols; j++) {
                    if (board.isCellClicked(i,j) && !board.hadMine(i,j)) {
                        onCellConfirm(i, j, board, gridLayout, textSize, player);
                    }
                }
            }
            // mine is not found show the indication number and safe cell is 0
        } else {
            int mineCountLeft = board.countMindAroundLeft(row, col);
            int mineCountTotal = board.countMindAroundTotal(row, col);
            // only update number if it has not clicked
            if (!board.isCellClicked(row, col)) {
                button.setText(String.valueOf(mineCountTotal));
                button.setTextSize(textSize);
                button.setPadding(5, 5, 5, 5);
            }

            // Set the cell as clicked and disable clicking
            button.setEnabled(false); // disable click

            // If mine count is 0, change the surrounding 8 cells' color
            if (mineCountLeft == 0) {
                for (int i = row - 1; i <= row + 1; i++) {
                    for (int j = col - 1; j <= col + 1; j++) {
                        // Only change the color for valid positions within the grid and skip the center cell
                        if (i >= 0 && i < board.getRows() && j >= 0 && j < board.getCols() && !board.hadMine(i,j) ) {

                            Button surroundingButton = (Button) gridLayout.getChildAt(i * boardCols + j);
                            if ((i + j) % 2 == 0) {
                                surroundingButton.setBackgroundColor(Color.parseColor("#032058"));
                            } else {
                                surroundingButton.setBackgroundColor(Color.parseColor("#4f638a"));
                            }
                            //button.setTextColor(Color.WHITE);



                        }
                    }
                }
            }
        }

        board.clickCell(row, col);

    }

    private void botMove() {
        int[] move = bot.attack(myBoard);
        onCellConfirm(move[0], move[1], myBoard, myGridLayout, 10, true);
    }

    private void launchConfirm() {
        if (myTurn && lastSelectedButton != null) {
            countDownTimer.cancel();
            onCellConfirm(selectedRow, selectedCol, botBoard, botGridLayout, 20, false);
            if (lastSelectedButton != null) {
                selectTextHolder = lastSelectedButton.getText().toString();
            }
            lastSelectedButton = null;
            myTurn = false;

            // Introduce a delay before the bot makes its move
            Random random = new Random();
            //int randomDelay = random.nextInt(2000) + 1500;
            int randomDelay = random.nextInt(500); // set low for testing

            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    botMove(); // Bot's move after the delay
                    if (lastSelectedButton != null) {
                        lastSelectedButton.setText(selectTextHolder); // rest the last button
                    }
                    myTurn = true;
                }
            }, randomDelay);
            countDownTimer.start();
        }
    }

}