package edu.northeastern.finalprojectgroup11;

import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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

import edu.northeastern.finalprojectgroup11.Music.BGMPlayer2;

public class DeployActivity extends AppCompatActivity {
    private String TAG = "DeployActivity";
    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth mAuth;

    private String roomCode;
    private String roomType;
    private String UID;
    private String opponentUID;
    private DatabaseReference roomRef;
    Button btnReady;

    private ValueEventListener bothReady;
    private ValueEventListener opponentStateListener;

    private boolean isBoatPlaced = false;

    private GameBoard board;
    private androidx.gridlayout.widget.GridLayout gridLayout;
    private int rows = 10;
    private int cols = 10;
    private Random random = new Random();
    private Handler handler; // used for delay

    private CountDownTimer countDownTimer;
    private TextView countdownTextView; // TextView to show the countdown


    private TextView mineLeftextView;

    @Override
    protected void onDestroy() {
        super.onDestroy();

//        BGMPlayer2.getInstance(this).stop();


        // Remove event listener
        if (bothReady != null) {
            roomRef.child("players").removeEventListener(bothReady);
        }
        if (opponentStateListener != null) {
            roomRef.child("players").child(opponentUID).child("playerState").removeEventListener(opponentStateListener);
        }
        if (countDownTimer != null) {
            countDownTimer.cancel();
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
                            if ("win".equals(currentState) || "lose".equals(currentState)) {
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
    protected void onPause() {
        super.onPause();
//        BGMPlayer2.getInstance(this).pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        BGMPlayer2.getInstance(this).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bot_deploy);

        handler = new Handler(Looper.getMainLooper()); // for delay

        mineLeftextView = findViewById(R.id.mineLeftTextView);
        countdownTextView = findViewById(R.id.countdownTextView);
        btnReady = findViewById(R.id.buttonReady);
        Button btnRandom = findViewById(R.id.buttonRandom);
        Button btnReset = findViewById(R.id.buttonReset);
        Button btnQuit = findViewById(R.id.buttonQuit);


        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showQuitConfirmationDialog(); // Show the same quit confirmation dialog
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

//        BGMPlayer2.getInstance(this).start();


        // Retrieve room code
        Intent intent = getIntent();
        roomCode = intent.getStringExtra("roomCode");

        // Link to firebase
        firebaseDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        UID = mAuth.getCurrentUser().getUid();
        roomType = intent.getStringExtra("roomType");
        roomRef = firebaseDatabase.getReference(roomType).child(roomCode);

        // Initialize the countdown timer for 10 seconds
        countDownTimer = new CountDownTimer(20000, 1000) {
            public void onTick(long millisUntilFinished) {
                // Update the countdown text each second
                countdownTextView.setText("" + millisUntilFinished / 1000);
            }

            public void onFinish() {
                placeRandom();
                Toast.makeText(DeployActivity.this, "Time's up! Random move selected.", Toast.LENGTH_SHORT).show();
                // Prevent user click after random select
                setGridLayoutEnabled(false);

                // Add delay
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setGridLayoutEnabled(true); // reset to true
                        onReadyClick();
                    }
                }, 1500);

            }
        };
        // Start the countdown when the activity is created
        countDownTimer.start();

        // Ready button to start game
        btnReady.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onReadyClick();
            }
        });

        // Set current player as not ready as default
        roomRef.child("players").child(UID).child("playerState").setValue("deploying");

        // Listener to check if both ready and enter battle
        bothReady = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                checkReady(roomRef);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DeployActivity.this, "Failed to check players' status.", Toast.LENGTH_SHORT).show();
            }
        };
        roomRef.child("players").addValueEventListener(bothReady);

        // Random button to random place mine
        btnRandom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onResetClick();
                placeRandom();
            }
        });

        // Reset button to clear the mine on the board
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onResetClick();
            }
        });

        // Button to quit game
        btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQuitConfirmationDialog();
            }
        });

        // Board stuff
        this.board = new GameBoard(rows, cols);
        gridLayout = findViewById(R.id.gridLayoutMinePlacement);
        int totalWidth = gridLayout.getWidth();
        int buttonSize = totalWidth / cols;
        int remainder = totalWidth % cols; // Calculate any remainder pixels
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Button button = new Button(this);

                button.setBackgroundResource(R.drawable.button_border);

                androidx.gridlayout.widget.GridLayout.LayoutParams params = new androidx.gridlayout.widget.GridLayout.LayoutParams(
                        androidx.gridlayout.widget.GridLayout.spec(i, 1f), GridLayout.spec(j, 1f));
                params.width = buttonSize + (j < remainder ? 1 : 0); // Distribute the remainder pixels
                params.height = params.width;
                button.setLayoutParams(params);

                // Set color based on checkerboard pattern
                if ((i + j) % 2 == 0) {
                    button.setBackgroundColor(Color.parseColor("#2980fb"));
                    button.setTextColor(Color.WHITE); // Set text color for visibility
                } else {
                    button.setBackgroundColor(Color.parseColor("#97C0FB"));
                    button.setTextColor(Color.BLACK); // Set text color for visibility
                }

                gridLayout.addView(button);

                // Set click action here
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

        // Get information of the opponent
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Get UID of the opponent
                if (Objects.equals(snapshot.child("player1").getValue(String.class), UID)) {
                    opponentUID = snapshot.child("player2").getValue(String.class);
                } else {
                    opponentUID = snapshot.child("player1").getValue(String.class);
                }

                // Listen to opponent's state changes
                DatabaseReference opponentStateRef = roomRef.child("players").child(opponentUID).child("playerState");
                opponentStateListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String state = snapshot.getValue(String.class);
                        if ("lose".equals(state)) {
                            showYouWinDialog();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Failed to read opponent's state: " + error.getMessage());
                    }
                };
                opponentStateRef.addValueEventListener(opponentStateListener);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });

    }
    // Action when the ready button is clicked
    public void onReadyClick() {
        if (board.isDeployReady()) {
            // Cancel the timer
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            roomRef.child("players").child(UID).child("playerState").setValue("ready");
        } else {
            Toast.makeText(DeployActivity.this, "Deploy all the mines before start.", Toast.LENGTH_SHORT).show();
        }
    }
    // 1 mine is placed, -1 mine is removed
    public int onCellClick(int row, int col) {
        Button button = (Button) gridLayout.getChildAt(row * board.getCols() + col);

        if (!board.hasMine(row, col) && board.getMineToPlace() > 0) {
            board.placeMine(row, col);

            // Update the view to show the mine placement
            button.setText("\u26AB");
            button.setGravity(Gravity.CENTER);
            button.setTextSize(20);
            button.setPadding(5, 5, 5, 5);
            mineLeftextView.setText(String.valueOf(board.getMineToPlace()));

            return 1;

        } else {
            board.removeMineDeploy(row,col);
            board.removeMine(row,col);
            button.setText(""); // Mark the mine
            mineLeftextView.setText(String.valueOf(board.getMineToPlace()));
            return -1;
        }
    }

    // Check if both players are ready and start game activity if ready
    private void checkReady(DatabaseReference roomRef) {
        roomRef.child("players").addListenerForSingleValueEvent(new ValueEventListener() {
            int readyCount = 0;

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot playerSnapshot : snapshot.getChildren()) {
                    String ready = playerSnapshot.child("playerState").getValue(String.class);
                    assert ready != null;
                    if (ready.equals("ready")) {
                        readyCount++;
                    }
                }
                if (readyCount == 2) {
                    // Retrieve mine locations
                    List<String> mineLocations = board.getAllMineLocations();
                    // Store them in Firebase under each player's node
                    roomRef.child("players").child(UID).child("locations").setValue(mineLocations)
                            .addOnSuccessListener(aVoid -> {
                                // Proceed to start the game
                                Toast.makeText(DeployActivity.this, "game should start.", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(DeployActivity.this, BattleActivity.class);
                                intent.putExtra("roomCode",roomCode); // Pass the room code into new activity
                                intent.putExtra("roomType", roomType); // Pass the room type (public/private) into new activity
                                GameBoardManager.setGameBoard(board);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to store mine locations: " + e.getMessage());
                            });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DeployActivity.this, "Failed to update players' status.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    // Reset the board by click cell with mine
    public void onResetClick() {
        for (int i=0; i<rows; i++) {
            for (int j=0; j<cols; j++) {
                if (board.hasMine(i,j)) {
                    onCellClick(i,j);
                }
            }
        }
    }
    // Randomly place mines
    public void placeRandom() {
        // Randomly place mines on the current board
        int minesToPlace = board.getMineToPlace();
        while (minesToPlace > 0) {
            int row = random.nextInt(rows);
            int col = random.nextInt(cols);
            if (onCellClick(row, col) == 1) {
                minesToPlace--;
            } else {
                minesToPlace++;
            }
        }
    }
    private void setGridLayoutEnabled(boolean enabled) {
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View child = gridLayout.getChildAt(i);
            child.setEnabled(enabled);
        }
    }

    private void showQuitConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit")
                .setMessage("Are you sure you want to quit the game?")
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
        Intent intent = new Intent(DeployActivity.this, MainActivity.class);
        intent.putExtra("roomType", roomType); // Pass the room type (public/private) into new activity
        startActivity(intent);
        finish();
    }
}
