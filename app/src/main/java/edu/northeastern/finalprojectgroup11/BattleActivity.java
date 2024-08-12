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
import android.view.ViewTreeObserver;
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
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
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
    // All the listener
    private ValueEventListener turnListener;
    private ValueEventListener opponentStateListener;
    private ValueEventListener meGetHitListener;

    private final int botDelay = 0;
    private final int boardRows = 10;
    private final int boardCols = 10;
    private GameBoard myBoard;
    private androidx.gridlayout.widget.GridLayout myGridLayout;
//    private BotPlayer bot;
    private GameBoard botBoard;
    private androidx.gridlayout.widget.GridLayout botGridLayout;
    private Handler handler; // Handler for managing delays
    private boolean myTurn;
    private final int round = 15;
    private int myRoundLeft = round;
    private int botRoundLeft = round;

    private CountDownTimer countDownTimer;
    private TextView countdownTextView; // TextView to show the countdown

    private TextView bigMineTextView;
    private TextView bigRoundLeftIconTextView;
    private TextView smallMineTextView;
    private TextView smallRoundLeftTextView;
    Button btnLaunch; // To set en/disable

    private int selectedRow = -1;
    private int selectedCol = -1;
    private Button lastSelectedButton = null; // To keep track of the last selected button
    String selectTextHolder; // hold the stuff on the cell

    // for clock animation
    private TextView bigCountdownIcon;
    private final String[] clockFaces = {
            "\uD83D\uDD5B",
            "\uD83D\uDD50",
            "\uD83D\uDD51",
            "\uD83D\uDD52",
            "\uD83D\uDD53",
            "\uD83D\uDD54",
            "\uD83D\uDD55",
            "\uD83D\uDD56",
            "\uD83D\uDD57",
            "\uD83D\uDD58",
            "\uD83D\uDD59",
            "\uD83D\uDD5A",
    };


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

        if (meGetHitListener != null) {
            roomRef.child("players").child(UID).child("meGetHit").removeEventListener(meGetHitListener);
        }

        // Delay the execution of the code by 5 second
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (roomRef != null && UID != null) {
                    // Check the current state of the user
                    roomRef.child("players").child(UID).child("playerState").addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String currentState = snapshot.getValue(String.class);
                            if ("win".equals(currentState) || "lose".equals(currentState) || "tie".equals(currentState)) {
                                roomRef.child("players").child(UID).child("playerState").setValue("quit");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Failed to read player's state: " + error.getMessage());
                        }
                    });
                }
            }
        }, 5000); // 5000 milliseconds delay
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bot_battle);

        // For all the view
        btnLaunch = findViewById(R.id.buttonLaunch);
        Button btnQuit = findViewById(R.id.buttonQuit);
        bigMineTextView = findViewById(R.id.bigMineTextView);
        bigRoundLeftIconTextView = findViewById(R.id.bigRoundLeftIconTextView);
        smallMineTextView = findViewById(R.id.smallMineTextView);
        smallRoundLeftTextView = findViewById(R.id.smallRoundLeftTextView);
        bigRoundLeftIconTextView.setText(String.valueOf(round));
        smallRoundLeftTextView.setText(String.valueOf(round));
        botGridLayout = findViewById(R.id.gridLayoutMinePlacement);
        myGridLayout = findViewById(R.id.gridLayoutMyMine);
        bigCountdownIcon = findViewById(R.id.bigCountdownIcon);

        // Set up both board first
        myBoard = GameBoardManager.getGameBoard(); // set my board directly from static class
        botBoard = GameBoardManager.getOpponentBoard();
        GameBoardManager.clearGameBoard(); // clear manager after retrieve board
        GameBoardManager.clearOpponentBoard();

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


        // Single value event to set up game and board from firebase data
        // UID and player position
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // call set up for UID, turn, put mine on board, and who start
                setupGameData(snapshot);

                // set up me get hit
                setupMeGetHitListener();



                // check who start the turn
                if (myTurn){
                    Toast.makeText(BattleActivity.this, "You start First!", Toast.LENGTH_SHORT).show();
                    countDownTimer.start();
                    setGridLayoutEnabled(true);
                } else {
                    Toast.makeText(BattleActivity.this, "Opponent start First!", Toast.LENGTH_SHORT).show();
                    setGridLayoutEnabled(false);
                }

                // Turn listener used to detect turn change
                setupTurnListener();

                // Notify winning when the opponent lose/quit
                setupOpponentStateListener();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });

        //drawBoard;
        drawBigBoard();
        drawSmallBoard();

        // button and view stuff below

        handler = new Handler(Looper.getMainLooper());
        countdownTextView = findViewById(R.id.countdownTextView); // Assuming you have this in your layout

        // Set round timer and auto hit
        setRoundTimer();

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

        // Link back button with quit
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showQuitConfirmationDialog(); // Show the same quit confirmation dialog
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

    }


    // set up UID, and turn
    private void setupGameData(DataSnapshot snapshot) {
        // Get UID for both side
        if (Objects.equals(snapshot.child("player1").getValue(String.class), UID)) {
            playerPosition = 1;
            opponentUID = snapshot.child("player2").getValue(String.class);
        } else {
            playerPosition = 2;
            opponentUID = snapshot.child("player1").getValue(String.class);
        }
        Log.d(TAG, "Opponent UID: " + opponentUID);

        // Set my turn base on room turn and UID
        if (Objects.equals(snapshot.child("turn").getValue(Integer.class), playerPosition)) {
            myTurn = true;
        }

    }


    // Draw board for both the big board
    public void drawBigBoard() {
        // Draw the Large game board
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
                        onCellClick(row, col);
                    }
                });
            }
        }
    }

    public void drawSmallBoard() {
        // Small, Draw the small game board
        int totalWidth = botGridLayout.getWidth();
        int buttonSize = totalWidth / boardCols;
        int remainder = totalWidth % boardCols;  // Calculate any remainder pixels
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
//                final int row = i;
//                final int col = j;
//                button.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        revealCell(row, col, myBoard, myGridLayout, 10, true);
//                    }
//                });
            }
        }
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
        if (lastSelectedButton != null) {
            // Stop the timer
            countDownTimer.cancel();

            // reveal the cell selected
            revealCell(selectedRow, selectedCol, botBoard, botGridLayout, 20, false);
            myRoundLeft--;
            bigMineTextView.setText(String.valueOf(botBoard.getMineLeftCount()));
            bigRoundLeftIconTextView.setText(String.valueOf(this.myRoundLeft));

            // Gog hit opponent at their firebase
            ArrayList<Integer> hitPosition = new ArrayList<>();
            hitPosition.add(selectedRow); // row
            hitPosition.add(selectedCol); // col
            roomRef.child("players").child(opponentUID).child("meGetHit").setValue(hitPosition);

            // Check game end will exist game if true
            if (!checkGameEnd()) {
                // Set up last button, turn, round, and click
                if (lastSelectedButton != null) {
                    selectTextHolder = lastSelectedButton.getText().toString();
                }
                lastSelectedButton = null;
                myTurn = false;
                // Set the Mine left and round left for big board





                // Start Counter and set turn to the opponent on firebase
                roomRef.child("turn").setValue(3 - currentTurn); // Switch turn
//                botMove();
            }
        }
    }


    // Used to disable click when the oppnent hit, skip the cell already clicked
    private void setGridLayoutEnabled(boolean enabled) {
        // Set Launch button
        btnLaunch.setEnabled(enabled);

        // Set the board
        for (int i = 0; i < botGridLayout.getChildCount(); i++) {
            int row = i / boardCols;
            int col = i % boardCols;

            if (!botBoard.isCellClicked(row, col)) {  // Skip the cell if it is already clicked
                View child = botGridLayout.getChildAt(i);
                child.setEnabled(enabled);
            }
        }
    }

    // only the loser show lose, so winner is set win automatically
    private boolean checkGameEnd() {
        // Check if all my mine get found
        if (myBoard.isAllFound()) {
            quitGame();
            return true;
        }
        // Check if all rounds are exhausted
        else if (myRoundLeft == 0 && botRoundLeft == 0) {
            if (botBoard.getMineLeftCount() > myBoard.getMineLeftCount()) {
                quitGame();
            } else if (botBoard.getMineLeftCount() == myBoard.getMineLeftCount()) {
                quitGameTie();
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


    private void quitGameTie() {
        if (UID != null && roomRef != null) {
            roomRef.child("players").child(UID).child("playerState").setValue("tie")
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            roomRef.child("players").child(opponentUID).child("playerState").setValue("tie")
                                    .addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            showTieDialog();
                                        }
                                    });
                        }
                    });
        }
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

    private void showLoseDialog() {
        countDownTimer.cancel();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("You Lose")
                .setMessage("You have lost the game.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateResult(false);
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
                        updateResult(true);
                        navigateToMain();
                    }
                })
                .setCancelable(false);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateResult(boolean win) {
        String resultType = win ? "win" : "loss";
        DatabaseReference resultRef = firebaseDatabase.getReference("users").child(UID).child(resultType);
        resultRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                Integer currentValue = mutableData.getValue(Integer.class);
                if (currentValue == null) {
                    mutableData.setValue(1); // Set to 1 if null (first win or loss)
                } else {
                    mutableData.setValue(currentValue + 1); // Increment the current value
                }
                return Transaction.success(mutableData); // Transaction success
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean committed, DataSnapshot dataSnapshot) {
                if (committed) {
                    Log.d(TAG, "updateResult Transaction committed. " + resultType + " updated successfully.");
                } else {
                    Log.e(TAG, "updateResult Transaction failed: " + databaseError.getMessage());
                }
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(BattleActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }


    // Round timer
    private void setRoundTimer() {        // Initialize the countdown timer for 10 seconds
        countDownTimer = new CountDownTimer(10000, 1000) {

            int clockIndex = 0;

            public void onTick(long millisUntilFinished) {
                // Update the countdown text each second
                countdownTextView.setText("" + millisUntilFinished / 1000);
                // Update the TextView with the next clock face symbol
                bigCountdownIcon.setText(clockFaces[clockIndex]);
                // Move to the next clock face
                clockIndex = (clockIndex + 1) % clockFaces.length;
            }

            public void onFinish() {
                if (myTurn) {
                    randomHit();
                }

            }
        };
    }

    private void randomHit() {
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


    // Listen to turn change and show who's turn and show time and dis/enable clicking
    private void setupTurnListener() {
        // Listen to turn changes
        turnListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentTurn = snapshot.getValue(Integer.class);
                if (Objects.equals(snapshot.getValue(Integer.class), playerPosition)) {
                    myTurn = true;
                    Toast.makeText(BattleActivity.this, "Your Turn", Toast.LENGTH_SHORT).show();
                    countDownTimer.start();
                    setGridLayoutEnabled(true);
                } else {
                    myTurn = false;
                    setGridLayoutEnabled(false);
                    countDownTimer.start();
                }
//                        updateTurnUI();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to read turn data: " + error.getMessage());
            }
        };
        roomRef.child("turn").addValueEventListener(turnListener);
    }


    private void setupOpponentStateListener() {
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
                    } else if ("tie".equals(state)) { // show Tie if other one is tie
                        showTieDialog();
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

    private void setupMeGetHitListener() {
        meGetHitListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Long> hitPosition = (ArrayList<Long>) snapshot.getValue();

                if (hitPosition != null && hitPosition.size() == 2) {
                    Integer row = hitPosition.get(0).intValue();
                    Integer col = hitPosition.get(1).intValue();

                    if (row != -1 && col != -1) {
                        Log.d(TAG, "Successfully retrieved row: " + row + ", col: " + col);
                        // mimic getting hit
                        botRoundLeft--;
                        revealCell(row, col, myBoard, myGridLayout, 10, true);
                        smallMineTextView.setText(String.valueOf(myBoard.getMineLeftCount()));
                        smallRoundLeftTextView.setText(String.valueOf(botRoundLeft));
                        revealCell(row, col, myBoard, myGridLayout, 10, true);
                        checkGameEnd();

                    }
                } else {
                    Log.e(TAG, "Failed to retrieve valid hit position.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to retrieve hit position: " + error.getMessage());
            }
        };
        roomRef.child("players").child(UID).child("meGetHit").addValueEventListener(meGetHitListener);
    }

}
