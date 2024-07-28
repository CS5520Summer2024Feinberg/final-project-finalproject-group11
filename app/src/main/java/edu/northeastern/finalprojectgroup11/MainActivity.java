package edu.northeastern.finalprojectgroup11;

import static android.content.ContentValues.TAG;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference userStatusRef;
    private Button btnLogin;
    private String currentUID;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPreferences = getSharedPreferences("Battleship", MODE_PRIVATE);
        currentUID = sharedPreferences.getString("UID", null);


        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        // Check if user is logged in and update UI accordingly
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUID != null) {
            setUserOnlineStatus(currentUID, true);
            DatabaseReference usernameRef = firebaseDatabase.getReference("users")
                    .child(currentUID)
                    .child("username");
            usernameRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // Get the username as a String
                    String username = dataSnapshot.getValue(String.class);
                    if (username != null) {
                        // Set the button text to the username
                        btnLogin.setText(username);
                    } else {
                        // Handle the case where username is not available
                        btnLogin.setText("User");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle potential errors here
                    Log.w("TAG", "Failed to read username.");
                }
            });
        }


        btnLogin = findViewById(R.id.login_btn);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentUID != null) {
                    showSignOutDialog();
                } else {
                    showLoginDialog();
                }
            }
        });

        Button btnCreateRoom = findViewById(R.id.createRoom_btn);
        btnCreateRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createRoom();
            }
        });

        Button btnJoinRoom = findViewById(R.id.joinRoom_btn);
        btnJoinRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showJoinRoomDialog();
            }
        });
    }

    //show the login dialog when press login button
    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View loginView = inflater.inflate(R.layout.dialog_signin, null);
        builder.setView(loginView)
                .setPositiveButton("Sign in", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // sign in though firebase
                        EditText usernameEditText = loginView.findViewById(R.id.username);
                        String username = usernameEditText.getText().toString().trim();
                        if (!username.isEmpty()) {
                            signInAnonymously(username);
                        } else {
                            Toast.makeText(MainActivity.this, "Please enter a username", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void signInAnonymously(String username) {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            currentUID = user.getUid();
                            setUserOnlineStatus(user.getUid(), true);
                            // Store username in the database
                            firebaseDatabase.getReference("users").child(user.getUid()).child("username").setValue(username);
                            btnLogin.setText(username);
                            sharedPreferences.edit().putString("UID", currentUID).apply();
                        }
                    } else {
                        Log.w(TAG, "signInAnonymously:failure", task.getException());
                        Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setUserOnlineStatus(String userId, boolean isOnline) {
        userStatusRef = firebaseDatabase.getReference("users").child(userId).child("status");
        if (isOnline) {
            userStatusRef.setValue("online");
            userStatusRef.onDisconnect().setValue("offline");
        } else {
            userStatusRef.setValue("offline");
        }
    }

    private void showSignOutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign out", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        signOut();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss(); // Dismiss the dialog
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void signOut() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            setUserOnlineStatus(user.getUid(), false);
            mAuth.signOut();
            btnLogin.setText("Login");
            sharedPreferences.edit().remove("UID").apply();
            currentUID = null;

        }
    }


    //create room stuff
    //
    private void createRoom() {
        String roomCode = generateRoomCode();  // Method to generate a unique room code
        DatabaseReference roomRef = firebaseDatabase.getReference("rooms").child(roomCode);
        roomRef.child("player1").setValue(currentUID);  // Save the current user's UID as player1
        roomRef.child("gameState").setValue("waiting"); // Set room state as waiting

        // Show the room code to the user to share with a friend
        showRoomCode(roomCode);

        // listener to detect player 2 join

        roomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Check if player2 has joined
                    String player2UID = snapshot.child("player2").getValue(String.class);
                    if (player2UID != null && !player2UID.isEmpty()) {
                        Toast.makeText(getApplicationContext(), "player 2 join, game should start", Toast.LENGTH_SHORT).show();
                        startGame(roomCode);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "listenForPlayer2Join:onCancelled", error.toException());
            }
        });

    }

    private String generateRoomCode() {
        return String.valueOf((int) (Math.random() * 10));  // Generate a random  number as room code
    }

    private void showRoomCode(String roomCode) {
        // Display room code in a dialog or on the screen
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Room Created")
                .setMessage("Share this code with your friend: " + roomCode)
                .setPositiveButton("OK", null)
                .show();
    }

    //join room

    private void showJoinRoomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_joinroom, null);
        builder.setView(dialogView)
                .setTitle("Join Room")
                .setPositiveButton("Join", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText editTextRoomCode = dialogView.findViewById(R.id.editTextRoomCode);
                        String roomCode = editTextRoomCode.getText().toString().trim();
                        if (!roomCode.isEmpty()) {
                            joinRoom(roomCode);
                        } else {
                            Toast.makeText(MainActivity.this, "Please enter a room code", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void joinRoom(String roomCode) {
        DatabaseReference roomRef = firebaseDatabase.getReference("rooms").child(roomCode);
        roomRef.child("player2").setValue(currentUID);  // Save the current user's UID as player2

        // Check if both players are present to start the game
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("player1").exists() && dataSnapshot.child("player2").exists()) {

                    Toast.makeText(getApplicationContext(), "join success, should start game", Toast.LENGTH_SHORT).show();
                    startGame(roomCode);
                } else {
                    Toast.makeText(getApplicationContext(), "Room not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "joinRoom:onCancelled", databaseError.toException());
            }
        });
    }

    private void startGame(String roomCode) {
        Intent intent = new Intent(MainActivity.this, DeployActivity.class);
        intent.putExtra("roomCode",roomCode); // Pass the room code into new activity
        startActivity(intent);
    }

}