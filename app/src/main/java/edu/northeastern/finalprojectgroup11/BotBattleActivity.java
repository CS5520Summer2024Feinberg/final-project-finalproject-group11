package edu.northeastern.finalprojectgroup11;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.os.CountDownTimer;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.gridlayout.widget.GridLayout;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

import edu.northeastern.finalprojectgroup11.Music.BGMPlayer;
import edu.northeastern.finalprojectgroup11.Music.BGMPlayer2;


// TODO:  step left, winner checking
// icon for mine, misses(step left) on both side, timer, dialog pop up, link quit with back
public class BotBattleActivity extends AppCompatActivity {
    private final int botDelay = 0;
    private final int boardRows = 10;
    private final int boardCols = 10;
    private GameBoard myBoard;
    private GridLayout myGridLayout;
    private BotPlayer bot;
    private GameBoard botBoard;
    private GridLayout botGridLayout;
    private Handler handler; // Handler for managing delays
    private boolean myTurn = true;
    private final int round = 15;
    private int myRoundLeft = round;
    private int botRoundLeft = round;

    private CountDownTimer countDownTimer;
    private TextView countdownTextView; // TextView to show the countdown

    private TextView bigMineTextView;
    private TextView bigRoundLeftIconTextView;
    private TextView smallMineTextView;
    private TextView smallRoundLeftTextView;

    private int selectedRow = -1;
    private int selectedCol = -1;
    private Button lastSelectedButton = null; // To keep track of the last selected button
    String selectTextHolder; // hold the stuff on the cell

    private static final String PREFS_NAME = "BGMSettings";
    private static final String KEY_BGM_VOLUME = "bgmVolume";
    private int bgmVolume = 50; // Default volume (50% of max volume)

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BGMPlayer2.getInstance(this).stop();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bot_battle);

        Button btnLaunch = findViewById(R.id.buttonLaunch);
        Button btnQuit = findViewById(R.id.buttonQuit);
        bigMineTextView = findViewById(R.id.bigMineTextView);
        bigRoundLeftIconTextView = findViewById(R.id.bigRoundLeftIconTextView);
        smallMineTextView = findViewById(R.id.smallMineTextView);
        smallRoundLeftTextView = findViewById(R.id.smallRoundLeftTextView);
        bigRoundLeftIconTextView.setText(String.valueOf(round));
        smallRoundLeftTextView.setText(String.valueOf(round));

        ImageButton btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(v -> showSettingsDialog());

        handler = new Handler(Looper.getMainLooper());
        countdownTextView = findViewById(R.id.countdownTextView); // Assuming you have this in your layout

        // Link back button with quit
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showQuitConfirmationDialog(); // Show the same quit confirmation dialog
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

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
                
//                // Show mine just for test
//                if (botBoard.hasMine(i, j)) {
//                    button.setText("M");
//                }

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

                 // Set click action, click on small board for test
                final int row = i;
                final int col = j;
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        revealCell(row, col, myBoard, myGridLayout, 10, true);
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

        // Quit button
        btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQuitConfirmationDialog();
            }
        });

    }

    private void showSettingsDialog() {
        // Load saved volume level
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        bgmVolume = sharedPreferences.getInt(KEY_BGM_VOLUME, 50); // Default to 50 if not set

        Dialog settingsDialog = new Dialog(this);
        settingsDialog.setContentView(R.layout.dialog_settings);
        settingsDialog.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        SeekBar seekBarVolume = settingsDialog.findViewById(R.id.seekBar_value);
        seekBarVolume.setProgress(bgmVolume);

        seekBarVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volume = progress / 100f; // Convert progress to a float between 0.0 and 1.0
                BGMPlayer2.getInstance(BotBattleActivity.this).setVolume(volume);

                // Save the volume level in SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(KEY_BGM_VOLUME, progress);
                editor.apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        Button btnHowToPlay = settingsDialog.findViewById(R.id.btn_howToPlay);
        btnHowToPlay.setOnClickListener(v -> {
            // Handle "How to Play" button click here
            // showHowToPlayDialog();
        });

        settingsDialog.show();
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


    // reveal cell, show mine if found, show number if not found
    private void revealCell(int row, int col, GameBoard board, GridLayout gridLayout, int textSize, boolean player) {  // 0 bot, 1 player
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
                        revealCell(i, j, board, gridLayout, textSize, player);
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


    // when launch is clicked
    private void launchConfirm() {
        if (myTurn && lastSelectedButton != null) {
            // Stop the timer
            countDownTimer.cancel();

            // reveal the cell selected
            revealCell(selectedRow, selectedCol, botBoard, botGridLayout, 20, false);
            myRoundLeft--;
            // Set the Mine left and round left for big board
            bigMineTextView.setText(String.valueOf(botBoard.getMineLeftCount()));
            bigRoundLeftIconTextView.setText(String.valueOf(this.myRoundLeft));

            // Check game end will exist game if true
            if (!checkGameEnd()) {
                // Set up last button, turn, round, and click
                if (lastSelectedButton != null) {
                    selectTextHolder = lastSelectedButton.getText().toString();
                }
                lastSelectedButton = null;
                myTurn = false;
                setGridLayoutEnabled(false);


                // Start Counter and let opponent move
                countDownTimer.start();
                botMove();
            }
        }
    }


    private void botMove() {
        int[] move = bot.attack(myBoard);
        Button button = (Button) myGridLayout.getChildAt(move[0] * boardCols + move[1]);
        button.setText("\u274C"); // this is a cross
        button.setTextColor(Color.parseColor("#E6301F"));
        button.setGravity(Gravity.CENTER);
        button.setTextSize(10);
        button.setPadding(1, 1, 1, 1);

        // Introduce a delay before the bot makes its move
        Random random = new Random();
        //int randomDelay = random.nextInt(2000) + 1500;
        int randomDelay = random.nextInt(100) + botDelay; // set low for testing
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                revealCell(move[0], move[1], myBoard, myGridLayout, 10, true);
                botRoundLeft--;
                // Set the Mine left and round left for big board
                smallMineTextView.setText(String.valueOf(myBoard.getMineLeftCount()));
                smallRoundLeftTextView.setText(String.valueOf(botRoundLeft));
                if (!checkGameEnd()) {

                    if (lastSelectedButton != null) {
                        lastSelectedButton.setText(selectTextHolder); // rest the last button
                    }
                    myTurn = true;



                    setGridLayoutEnabled(true);
                    countDownTimer.cancel();
                    countDownTimer.start();
                }

            }
        }, randomDelay);
    }


    // Used to disable click when the oppnent hit, skip the cell already clicked
    private void setGridLayoutEnabled(boolean enabled) {
        for (int i = 0; i < botGridLayout.getChildCount(); i++) {
            int row = i / boardCols;
            int col = i % boardCols;

            if (!botBoard.isCellClicked(row, col)) {  // Skip the cell if it is already clicked
                View child = botGridLayout.getChildAt(i);
                child.setEnabled(enabled);
            }
        }
    }


    private void showQuitConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Surrender")
                .setMessage("Are you sure you want to surrender?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showLoseDialog();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void showLoseDialog() {
        countDownTimer.cancel();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("You Lose")
                .setMessage("You have lost the game.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        navigateToMain();
                    }
                })
                .setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void showYouWinDialog() {
        countDownTimer.cancel();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Winner")
                .setMessage("Congratulations! You are the winner!")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        navigateToMain();
                    }
                })
                .setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void showTieDialog() {
        countDownTimer.cancel();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Tie Game")
                .setMessage("Tie Game")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        navigateToMain();
                    }
                })
                .setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void navigateToMain() {
        Intent intent = new Intent(BotBattleActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        BGMPlayer2.getInstance(this).stop();
        finish();
    }


    private boolean checkGameEnd() {
        // Check if all mines have been found on the bot's board (player wins)
        if (botBoard.isAllFound()) {
            showYouWinDialog();
            return true;
        }
        // Check if all my mine get found
        else if (myBoard.isAllFound()) {
            showLoseDialog();
            return true;
        }
        // Check if all rounds are exhausted
        else if (myRoundLeft == 0 && botRoundLeft == 0) {
            if (botBoard.getMineLeftCount() < myBoard.getMineLeftCount()) {
                showYouWinDialog();
            } else if (botBoard.getMineLeftCount() == myBoard.getMineLeftCount()) {
                showTieDialog();
            }
            else {
                showLoseDialog();
            }
            return true;
        }
        return false;
    }


}