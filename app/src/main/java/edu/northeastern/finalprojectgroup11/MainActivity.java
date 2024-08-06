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
    private DatabaseReference roomRef;
    private final int maxRoomCount = 2;
    private ValueEventListener player2Join;

    private AlertDialog roomDialog;


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

        Button btnTest = findViewById(R.id.test_btn);
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, dragDropDemo.class);
                startActivity(intent);
            }
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
                            checkIfUsernameExists(username);
                        } else {
                            Toast.makeText(MainActivity.this, "Please enter a username", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void checkIfUsernameExists(String username) {
        DatabaseReference usersRef = firebaseDatabase.getReference("users");
        usersRef.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Username exists, log in the user
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        currentUID = userSnapshot.getKey();
                        setUserOnlineStatus(currentUID, true);
                        sharedPreferences.edit().putString("UID", currentUID).apply();
                        btnLogin.setText(username);
                    }
                } else {
                    // Username does not exist, create a new user
                    signInAnonymously(username);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.w(TAG, "checkIfUsernameExists:onCancelled", databaseError.toException());
            }
        });
    }

    private void signInAnonymously(String username) {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            currentUID = user.getUid();
                            setUserOnlineStatus(user.getUid(), true);
                            storeUserData(user.getUid(), username);
                            btnLogin.setText(username);
                            sharedPreferences.edit().putString("UID", currentUID).apply();
                        }
                    } else {
                        Log.w(TAG, "signInAnonymously:failure", task.getException());
                        Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void storeUserData(String uid, String username) {
        DatabaseReference userRef = firebaseDatabase.getReference("users").child(uid);
        userRef.child("username").setValue(username)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User data stored successfully");
                    } else {
                        Log.w(TAG, "Failed to store user data", task.getException());
                    }
                });
        userRef.child("status").setValue("online");
    }

    private void setUserOnlineStatus(String userId, boolean isOnline) {
        userStatusRef = firebaseDatabase.getReference("users").child(userId).child("status");
        if (isOnline) {
            userStatusRef.setValue("online");
            userStatusRef.onDisconnect().setValue("offline");
        } else {
            userStatusRef.setValue("offline");
            Log.d(TAG, "User status set to offline for UID: " + userId);
        }
    }

    private void showSignOutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign out", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Sign out dialog confirmed");
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
        if (currentUID != null) {
            if (user != null) {
                mAuth.signOut();
            }
            setUserOnlineStatus(currentUID, false);
            currentUID = null;
            sharedPreferences.edit().remove("UID").apply();
            Log.d(TAG, "User signed out successfully");
            btnLogin.setText("Login");
        } else {
            Log.d(TAG, "No user to sign out");
        }
        Log.d(TAG, "currentUID after sign out: " + currentUID);
        Log.d(TAG, "SharedPreferences UID after sign out: " + sharedPreferences.getString("UID", "null"));
    }



    //create room stuff
    //
    private void createRoom() {
        String roomCode = generateRoomCode();  // Method to generate a random room code
        // check if the newly created room code exist;
        firebaseDatabase.getReference("rooms").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // If the server is full
                if (snapshot.getChildrenCount() == maxRoomCount) {
                    Toast.makeText(getApplicationContext(), "Game server is full, try later", Toast.LENGTH_LONG).show();
                }

                // if the room code is unique
                if (!snapshot.hasChild(roomCode)) {
                    // do the create room and show dialog stuff
                    roomRef = firebaseDatabase.getReference("rooms").child(roomCode);
                    roomRef.child("player1").setValue(currentUID);  // Save the current user's UID as player1
                    roomRef.child("gameState").setValue("waiting"); // Set room state as waiting

                    // Show the room code to the user to share with a friend
                    showRoomCode(roomCode);

                    // listener to detect player 2 join
                    player2Join = new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                // Check if player2 has joined
                                String player2UID = snapshot.child("player2").getValue(String.class);
                                if (player2UID != null && !player2UID.isEmpty()) {
                                    Toast.makeText(getApplicationContext(), "Player 2 joined, game should start", Toast.LENGTH_SHORT).show();
                                    if (roomDialog.isShowing() || roomDialog != null) {
                                        roomDialog.dismiss();
                                        roomDialog = null;
                                    }
                                    startGame(roomCode);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.w(TAG, "listenForPlayer2Join:onCancelled", error.toException());
                        }
                    };
                    roomRef.addValueEventListener(player2Join);
                } else {
                    // else keep create other number
                    Toast.makeText(getApplicationContext(), "repeat number", Toast.LENGTH_SHORT).show();
                    //createRoom();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });



    }

    private String generateRoomCode() {
        return String.valueOf((int) (Math.random() * maxRoomCount));  // Generate a random  number as room code
    }

    private void showRoomCode(String roomCode) {
        // Display room code in a dialog or on the screen
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Room Created")
                .setMessage("Share this code with your friend: " + roomCode)
                .setPositiveButton("Quit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        destroyRoom();
                        dialog.cancel();
                    }
                });
        roomDialog = builder.create();
        roomDialog.show();

    }

    // Destroy room when player1 quit from create room
    private void destroyRoom() {
        if (player2Join != null){
            roomRef.removeEventListener(player2Join);
        }

        if (roomRef != null) {
            roomRef.removeValue();
            roomRef = null;
        }
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
        // use a listener to check if roomcode exist or not
        firebaseDatabase.getReference("rooms").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // only join room if room exist
                if (snapshot.hasChild(roomCode)) {
                    roomRef = firebaseDatabase.getReference("rooms").child(roomCode);

                    // Check if both players are present to start the game
                    roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.child("player1").exists() && dataSnapshot.child("gameState").getValue().equals("waiting") ) {
                                roomRef.child("player2").setValue(currentUID);  // Save the current user's UID as player2
                                Toast.makeText(getApplicationContext(), "join success, should start game", Toast.LENGTH_SHORT).show();
                                startGame(roomCode);
                            } else {
                                Toast.makeText(getApplicationContext(), "Room is full", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            Log.w(TAG, "joinRoom:onCancelled", databaseError.toException());
                        }
                    });
                } else {
                    //else shoe room not found
                    Toast.makeText(getApplicationContext(), "Room not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });




    }

    private void startGame(String roomCode) {
        roomRef.child("gameState").setValue("active"); // Set room state as active

        // Remove listener for player 2 join
        if (player2Join != null){
            roomRef.removeEventListener(player2Join);
        }

        // start deploy activity
        Intent intent = new Intent(MainActivity.this, DeployActivity.class);
        intent.putExtra("roomCode",roomCode); // Pass the room code into new activity
        startActivity(intent);
    }

}