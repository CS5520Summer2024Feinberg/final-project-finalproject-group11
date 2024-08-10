package edu.northeastern.finalprojectgroup11;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.gridlayout.widget.GridLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class BattleActivity extends AppCompatActivity {
    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth mAuth;
    private DatabaseReference roomRef;
    private String UID;
    private String opponentUID;
    private String roomCode;
    private String roomType;
    private List<String> mineLocations;
    private List<String> opLocations;
    private int playerPosition;
    private int currentTurn;

    private ValueEventListener turnListener;
    private ValueEventListener opponentStateListener;


    private final int botDelay = 0;
    private final int boardRows = 10;
    private final int boardCols = 10;
    private GameBoard myBoard;
    private androidx.gridlayout.widget.GridLayout myGridLayout;
//    private BotPlayer bot;
    private GameBoard botBoard;
    private androidx.gridlayout.widget.GridLayout botGridLayout;
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


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove listeners
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        if (turnListener != null) {
            roomRef.child("turn").removeEventListener(turnListener);
        }
        if (opponentStateListener != null) {
            roomRef.child("players").child(opponentUID).child("playerState").removeEventListener(opponentStateListener);
        }

        // Delay the execution of the code by 1 second (1000 milliseconds)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (roomRef != null && UID != null) {
                    roomRef.child("players").child(UID).child("playerState").setValue("quit");
                }
            }
        }, 5000); // 1000 milliseconds delay
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bot_battle);

        // Retrieve room code
        Intent intent = getIntent();
        roomCode = intent.getStringExtra("roomCode");

        // Link to firebase
        firebaseDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        UID = mAuth.getCurrentUser().getUid();
        roomType = intent.getStringExtra("roomType");
        roomRef = firebaseDatabase.getReference(roomType).child(roomCode);
        Log.d(TAG, "Current UID: " + UID);
        Log.d(TAG, "Room code: " + roomCode);
        // Link back button with quit
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showQuitConfirmationDialog(); // Show the same quit confirmation dialog
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
        // Get information of the opponent
        // Set winning notification when having opponent information
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Get UID of the opponent
                if (Objects.equals(snapshot.child("player1").getValue(String.class), UID)) {
                    playerPosition = 1;
                    opponentUID = snapshot.child("player2").getValue(String.class);
                } else {
                    playerPosition = 2;
                    opponentUID = snapshot.child("player1").getValue(String.class);
                }

                Log.d(TAG, "Opponent UID: " + opponentUID);
                mineLocations = new ArrayList<String>();
                opLocations = new ArrayList<String>();
                // Fetch mine locations for both players
                for (DataSnapshot locationSnapshot : snapshot.child("players").child(UID).child("mines").getChildren()) {
                    mineLocations.add(locationSnapshot.getValue(String.class));
                }
                for (DataSnapshot locationSnapshot : snapshot.child("players").child(opponentUID).child("mines").getChildren()) {
                    opLocations.add(locationSnapshot.getValue(String.class));
                }

                Log.d(TAG, "Mines loaded for " + UID + ": " + mineLocations);
                Log.d(TAG, "Mines loaded for opponent " + opponentUID + ": " + opLocations);
                myBoard = new GameBoard(boardRows, boardCols);;
                botBoard = new GameBoard(boardRows, boardCols);;

                // Place player mines
                for (String location : mineLocations) {
                    String[] parts = location.split(",");
                    int row = Integer.parseInt(parts[0]);
                    int col = Integer.parseInt(parts[1]);
                    myBoard.placeMine(row, col);
                }

                // Place opponent mines
                for (String location : opLocations) {
                    String[] parts = location.split(",");
                    int row = Integer.parseInt(parts[0]);
                    int col = Integer.parseInt(parts[1]);
                    botBoard.placeMine(row, col);
                }


                // Listen to turn changes
                turnListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        currentTurn = snapshot.getValue(Integer.class);
//                        updateTurnUI();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to read turn data: " + error.getMessage());
                    }
                };
                roomRef.child("turn").addValueEventListener(turnListener);

                //Notify winning when the opponent lose/quit
                Log.d(TAG, "Setting up ValueEventListener on opponent's playerState for opponentUID: " + opponentUID);
                DatabaseReference playerStateRef = roomRef.child("players").child(opponentUID).child("playerState");
                opponentStateListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String state = snapshot.getValue(String.class);
                            Log.d(TAG, "Opponent's playerState: " + state);
                            if ("lose".equals(state)) { // show you win if the other one lose
                                showYouWinDialog();
                            }
                        } else {
                            Log.e(TAG, "DataSnapshot for playerState does not exist.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to read player's state: " + error.getMessage());
                    }
                };
                playerStateRef.addValueEventListener(opponentStateListener);


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });

        Button btnLaunch = findViewById(R.id.buttonLaunch);
        Button btnQuit = findViewById(R.id.buttonQuit);
        bigMineTextView = findViewById(R.id.bigMineTextView);
        bigRoundLeftIconTextView = findViewById(R.id.bigRoundLeftIconTextView);
        smallMineTextView = findViewById(R.id.smallMineTextView);
        smallRoundLeftTextView = findViewById(R.id.smallRoundLeftTextView);
        bigRoundLeftIconTextView.setText(String.valueOf(round));
        smallRoundLeftTextView.setText(String.valueOf(round));

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
                    Toast.makeText(BattleActivity.this, "Time's up! Random move selected.", Toast.LENGTH_SHORT).show();
                }
            }
        };

        // Start the countdown when the activity is created
        countDownTimer.start();

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
                androidx.gridlayout.widget.GridLayout.LayoutParams params = new androidx.gridlayout.widget.GridLayout.LayoutParams(
                        androidx.gridlayout.widget.GridLayout.spec(i, 1f), androidx.gridlayout.widget.GridLayout.spec(j, 1f));
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
                androidx.gridlayout.widget.GridLayout.LayoutParams params = new androidx.gridlayout.widget.GridLayout.LayoutParams(
                        androidx.gridlayout.widget.GridLayout.spec(i, 1f), androidx.gridlayout.widget.GridLayout.spec(j, 1f));
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

            // Check game end will exist game if true
            if (!checkGameEnd()) {
                // Set up last button, turn, round, and click
                if (lastSelectedButton != null) {
                    selectTextHolder = lastSelectedButton.getText().toString();
                }
                lastSelectedButton = null;
                myTurn = false;
                setGridLayoutEnabled(false);
                // Set the Mine left and round left for big board
                bigMineTextView.setText(String.valueOf(botBoard.getMineLeftCount()));
                bigRoundLeftIconTextView.setText(String.valueOf(this.myRoundLeft));

                // Start Counter and let opponent move
                countDownTimer.start();
//                botMove();
            }
        }
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
            } else {
                showLoseDialog();
            }
            return true;
        }
        return false;
    }

    private void showQuitConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Surrender")
                .setMessage("Are you sure you want to surrender?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        quitGame();
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

    private void quitGame() {
        if (UID != null && roomRef != null) {
            roomRef.child("players").child(UID).child("playerState").setValue("lose")
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            roomRef.child("players").child(opponentUID).child("playerState").setValue("win")
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            showLoseDialog();
                                        }
                                    });
                        }
                    });
        }
    }

    private void showLoseDialog() {
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

    private void navigateToMain() {
        Intent intent = new Intent(BattleActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }


}

//public class BattleActivity extends AppCompatActivity {
//    private FirebaseDatabase firebaseDatabase;
//    private FirebaseAuth mAuth;
//    private DatabaseReference roomRef;
//    private String UID;
//    private String opponentUID;
//    private String roomCode;
//    private String roomType;
//    private int boat1row;
//    private int boat1col;
//    private int playerPosition;
//    private int currentTurn;
//
//    private ValueEventListener turnListener;
//    private ValueEventListener opponentStateListener;
//
//
//
//    private TextView turnTextView;
//    private Button btnQuit;
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//
//        // Remove listeners
//        if (turnListener != null) {
//            roomRef.child("turn").removeEventListener(turnListener);
//        }
//        if (opponentStateListener != null) {
//            roomRef.child("players").child(opponentUID).child("playerState").removeEventListener(opponentStateListener);
//        }
//
//        // Delay the execution of the code by 1 second (1000 milliseconds)
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                if (roomRef != null && UID != null) {
//                    roomRef.child("players").child(UID).child("playerState").setValue("quit");
//                }
//            }
//        }, 5000); // 1000 milliseconds delay
//    }
//
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_battle);
//        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
//            @Override
//            public void handleOnBackPressed() {
//                showQuitConfirmationDialog(); // Show the same quit confirmation dialog
//            }
//        };
//        getOnBackPressedDispatcher().addCallback(this, callback);
//        // Retrieve room code
//        Intent intent = getIntent();
//        roomCode = intent.getStringExtra("roomCode");
//
//        // Link to firebase
//        firebaseDatabase = FirebaseDatabase.getInstance();
//        mAuth = FirebaseAuth.getInstance();
//        UID = mAuth.getCurrentUser().getUid();
//        roomType = intent.getStringExtra("roomType");
//        roomRef = firebaseDatabase.getReference(roomType).child(roomCode);
//        Log.d(TAG, "Current UID: " + UID);
//        Log.d(TAG, "Room code: " + roomCode);
//
//
//        // Get information of the opponent
//        // Set winning notification when having opponent information
//        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                // Get UID of the opponent
//                if (Objects.equals(snapshot.child("player1").getValue(String.class), UID)) {
//                    playerPosition = 1;
//                    opponentUID = snapshot.child("player2").getValue(String.class);
//                } else {
//                    playerPosition = 2;
//                    opponentUID = snapshot.child("player1").getValue(String.class);
//                }
//
//                Log.d(TAG, "Opponent UID: " + opponentUID);
//
//                // Get opponent boat location
//                boat1row = snapshot.child("players").child(opponentUID).child("boat1Location").child("row").getValue(Integer.class);
//                boat1col = snapshot.child("players").child(opponentUID).child("boat1Location").child("col").getValue(Integer.class);
//
//                // Listen to turn changes
//                turnListener = new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        currentTurn = snapshot.getValue(Integer.class);
//                        updateTurnUI();
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        Log.e(TAG, "Failed to read turn data: " + error.getMessage());
//                    }
//                };
//                roomRef.child("turn").addValueEventListener(turnListener);
//
//                //Notify winning when the opponent lose/quit
//                Log.d(TAG, "Setting up ValueEventListener on opponent's playerState for opponentUID: " + opponentUID);
//                DatabaseReference playerStateRef = roomRef.child("players").child(opponentUID).child("playerState");
//                opponentStateListener = new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        if (snapshot.exists()) {
//                            String state = snapshot.getValue(String.class);
//                            Log.d(TAG, "Opponent's playerState: " + state);
//                            if ("lose".equals(state)) { // show you win if the other one lose
//                                showYouWinDialog();
//                            }
//                        } else {
//                            Log.e(TAG, "DataSnapshot for playerState does not exist.");
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        Log.e(TAG, "Failed to read player's state: " + error.getMessage());
//                    }
//                };
//                playerStateRef.addValueEventListener(opponentStateListener);
//
//
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Log.e(TAG, "Database error: " + error.getMessage());
//            }
//        });
//
//
//        // Initialize turnTextView
//        turnTextView = findViewById(R.id.turnTextView);
//
//        // Randomly determine who goes first
//        Random random = new Random();
//        int firstTurn = random.nextInt(2) + 1; // 1 or 2
//        roomRef.child("turn").setValue(firstTurn);
//
//        // Grid layout stuff
//        GridLayout opponentGrid = findViewById(R.id.opponentGrid);
//        for (int i = 0; i < opponentGrid.getChildCount(); i++) {
//            View block = opponentGrid.getChildAt(i);
//            int row = i / opponentGrid.getColumnCount();
//            int col = i % opponentGrid.getRowCount();
//            block.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    // Current player selects a block to hit
//                    if (currentTurn == playerPosition) {
//                        if (row == boat1row && col == boat1col) {
//                            v.setBackgroundColor(ContextCompat.getColor(BattleActivity.this, android.R.color.holo_red_light));
//                        } else {
//                            v.setBackgroundColor(ContextCompat.getColor(BattleActivity.this, android.R.color.holo_blue_dark));
//                        }
//                        roomRef.child("turn").setValue(3 - currentTurn); // Switch turn
//                    }
//                }
//            });
//        }
//        updateTurnUI();
//
//        // Quit button
//        btnQuit = findViewById(R.id.buttonQuit);
//        btnQuit.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showQuitConfirmationDialog();
//            }
//        });
//
//    }
//
//    private void updateTurnUI() {
//        GridLayout opponentGrid = findViewById(R.id.opponentGrid);
//        if (currentTurn == playerPosition) {
//            turnTextView.setText("Your Turn");
//            setGridEnabled(opponentGrid, true);
//        } else {
//            turnTextView.setText("Opponent's Turn");
//            setGridEnabled(opponentGrid, false);
//        }
//    }
//
//    private void setGridEnabled(GridLayout grid, boolean enabled) {
//        for (int i = 0; i < grid.getChildCount(); i++) {
//            grid.getChildAt(i).setEnabled(enabled);
//        }
//    }
//
//    private void showQuitConfirmationDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Surrender")
//                .setMessage("Are you sure you want to surrender?")
//                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        quitGame();
//                    }
//                })
//                .setNegativeButton("No", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                })
//                .setCancelable(false);
//
//        AlertDialog dialog = builder.create();
//        dialog.show();
//    }
//
//    private void quitGame() {
//        if (UID != null && roomRef != null) {
//            roomRef.child("players").child(UID).child("playerState").setValue("lose")
//                    .addOnCompleteListener(task -> {
//                        if (task.isSuccessful()) {
//                            roomRef.child("players").child(opponentUID).child("playerState").setValue("win")
//                                    .addOnCompleteListener(task1 -> {
//                                        if (task1.isSuccessful()) {
//                                            showLoseDialog();
//                                        }
//                                    });
//                        }
//                    });
//        }
//    }
//
//    private void showLoseDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("You Lose")
//                .setMessage("You have lost the game.")
//                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        navigateToMain();
//                    }
//                })
//                .setCancelable(false);
//
//        AlertDialog dialog = builder.create();
//        dialog.show();
//    }
//
//    private void showYouWinDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Winner")
//                .setMessage("Congratulations! You are the winner!")
//                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        navigateToMain();
//                    }
//                })
//                .setCancelable(false);
//        AlertDialog dialog = builder.create();
//        dialog.show();
//    }
//
//    private void navigateToMain() {
//        Intent intent = new Intent(BattleActivity.this, MainActivity.class);
//        startActivity(intent);
//        finish();
//    }
//
//
//}

