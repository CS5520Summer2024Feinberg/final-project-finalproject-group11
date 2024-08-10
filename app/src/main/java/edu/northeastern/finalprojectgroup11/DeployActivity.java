package edu.northeastern.finalprojectgroup11;

import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.DragEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DeployActivity extends AppCompatActivity {
    private String TAG = "DeployActivity";
    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth mAuth;

    private String roomCode;
    private String roomType;
    private String UID;
    private String opponentUID;
    private DatabaseReference roomRef;
    private TextView dragTextView;
    Button btnReady;

    private ValueEventListener bothReady;
    private ValueEventListener opponentStateListener;

    private boolean isBoatPlaced = false;


    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Remove event listener
        if (bothReady != null) {
            roomRef.child("players").removeEventListener(bothReady);
        }
        if (opponentStateListener != null) {
            roomRef.child("players").child(opponentUID).child("playerState").removeEventListener(opponentStateListener);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_deploy);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showQuitConfirmationDialog(); // Show the same quit confirmation dialog
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
        // Retrieve room code
        Intent intent = getIntent();
        roomCode = intent.getStringExtra("roomCode");

        // Link to firebase
        firebaseDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        UID = mAuth.getCurrentUser().getUid();
        roomType = intent.getStringExtra("roomType");
        roomRef = firebaseDatabase.getReference(roomType).child(roomCode);

        // show room code
        TextView roomCodeTextView = findViewById(R.id.textViewRoom);
        roomCodeTextView.setText(roomCode);

        // Set current player as not ready as default
        roomRef.child("players").child(UID).child("playerState").setValue("deploying");

        // Button to set ready
        btnReady = findViewById(R.id.buttonReady);
        btnReady.setEnabled(false);
        btnReady.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isBoatPlaced) {
                    roomRef.child("players").child(UID).child("playerState").setValue("ready");
                } else {
                    Toast.makeText(DeployActivity.this, "Please place the boat on the grid before readying up.", Toast.LENGTH_SHORT).show();
                }            }
        });

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

        // Button to quit game
        Button btnQuit = findViewById(R.id.buttonQuit);
        btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQuitConfirmationDialog();
            }
        });

        // Drag stuff
        // Set up the drag listener for the TextView
        dragTextView = findViewById(R.id.dragTextView);

        dragTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipData data = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(v);
                v.startDragAndDrop(data, shadowBuilder, v, 0);
                return true;
            }
        });

        // Set up the drag listener for the GridLayout
        GridLayout gridLayout = findViewById(R.id.gridLayout);
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View block = gridLayout.getChildAt(i);
            block.setOnDragListener(new DeployActivity.MyDragListener());
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
                    Toast.makeText(DeployActivity.this, "game should start.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(DeployActivity.this, BattleActivity.class);
                    intent.putExtra("roomCode",roomCode); // Pass the room code into new activity
                    intent.putExtra("roomType", roomType); // Pass the room type (public/private) into new activity
                    startActivity(intent);
                    finish();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DeployActivity.this, "Failed to update players' status.", Toast.LENGTH_SHORT).show();
            }
        });
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

    private class MyDragListener implements View.OnDragListener {

        @Override
        public boolean onDrag(View v, DragEvent event) {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return true;

                case DragEvent.ACTION_DRAG_ENTERED:
                    v.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_light)); // Highlight the target
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    v.setBackgroundColor(getResources().getColor(android.R.color.darker_gray)); // Revert the highlight
                    return true;

                case DragEvent.ACTION_DROP:
                    View draggedView = (View) event.getLocalState();
                    // Calculate the center position of the target block
                    float targetX = v.getX() + (v.getWidth() - draggedView.getWidth()) / 2;
                    float targetY = v.getY() + (v.getHeight() - draggedView.getHeight()) / 2;
                    draggedView.setX(targetX);
                    draggedView.setY(targetY);

                    // Get the location of the block
                    String location = ((TextView) v).getText().toString();
                    // Convert location string "[row,col]" to List<Integer>
                    String[] parts = location.replaceAll("[\\[\\]]", "").split(",");
                    List<Integer> coordinates = new ArrayList<>();
                    //coordinates.add(Integer.parseInt(parts[0].trim()));
                    //coordinates.add(Integer.parseInt(parts[1].trim()));
                    // Update the boat location in the database
                    roomRef.child("players").child(UID).child("boat1Location").child("row").setValue(Integer.parseInt(parts[0].trim()));
                    roomRef.child("players").child(UID).child("boat1Location").child("col").setValue(Integer.parseInt(parts[1].trim()));

                    isBoatPlaced = true;
                    btnReady.setEnabled(true);

                    return true;


                case DragEvent.ACTION_DRAG_ENDED:
                    v.setBackgroundColor(ContextCompat.getColor(DeployActivity.this, android.R.color.darker_gray)); // Revert the highlight
                    return true;

                default:
                    break;
            }
            return false;
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
