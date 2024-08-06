package edu.northeastern.finalprojectgroup11;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;
import java.util.Random;

public class BattleActivity extends AppCompatActivity {
    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth mAuth;
    private DatabaseReference roomRef;
    private String UID;
    private String opponentUID;
    private String roomCode;
    private int boat1row;
    private int boat1col;
    private int playerPosition;
    private int currentTurn;

    private ValueEventListener turnListener;
    private ValueEventListener opponentStateListener;



    private TextView turnTextView;
    private Button btnQuit;

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove listeners
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
        setContentView(R.layout.activity_battle);

        // Retrieve room code
        Intent intent = getIntent();
        roomCode = intent.getStringExtra("roomCode");

        // Link to firebase
        firebaseDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        UID = mAuth.getCurrentUser().getUid();
        roomRef = firebaseDatabase.getReference("rooms").child(roomCode);
        Log.d(TAG, "Current UID: " + UID);
        Log.d(TAG, "Room code: " + roomCode);


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

                // Get opponent boat location
                boat1row = snapshot.child("players").child(opponentUID).child("boat1Location").child("row").getValue(Integer.class);
                boat1col = snapshot.child("players").child(opponentUID).child("boat1Location").child("col").getValue(Integer.class);

                // Listen to turn changes
                turnListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        currentTurn = snapshot.getValue(Integer.class);
                        updateTurnUI();
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


        // Initialize turnTextView
        turnTextView = findViewById(R.id.turnTextView);

        // Randomly determine who goes first
        Random random = new Random();
        int firstTurn = random.nextInt(2) + 1; // 1 or 2
        roomRef.child("turn").setValue(firstTurn);

        // Grid layout stuff
        GridLayout opponentGrid = findViewById(R.id.opponentGrid);
        for (int i = 0; i < opponentGrid.getChildCount(); i++) {
            View block = opponentGrid.getChildAt(i);
            int row = i / opponentGrid.getColumnCount();
            int col = i % opponentGrid.getRowCount();
            block.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Current player selects a block to hit
                    if (currentTurn == playerPosition) {
                        if (row == boat1row && col == boat1col) {
                            v.setBackgroundColor(ContextCompat.getColor(BattleActivity.this, android.R.color.holo_red_light));
                        } else {
                            v.setBackgroundColor(ContextCompat.getColor(BattleActivity.this, android.R.color.holo_blue_dark));
                        }
                        roomRef.child("turn").setValue(3 - currentTurn); // Switch turn
                    }
                }
            });
        }
        updateTurnUI();

        // Quit button
        btnQuit = findViewById(R.id.buttonQuit);
        btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQuitConfirmationDialog();
            }
        });

    }

    private void updateTurnUI() {
        GridLayout opponentGrid = findViewById(R.id.opponentGrid);
        if (currentTurn == playerPosition) {
            turnTextView.setText("Your Turn");
            setGridEnabled(opponentGrid, true);
        } else {
            turnTextView.setText("Opponent's Turn");
            setGridEnabled(opponentGrid, false);
        }
    }

    private void setGridEnabled(GridLayout grid, boolean enabled) {
        for (int i = 0; i < grid.getChildCount(); i++) {
            grid.getChildAt(i).setEnabled(enabled);
        }
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
                });

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
                });

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
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void navigateToMain() {
        Intent intent = new Intent(BattleActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }


}
