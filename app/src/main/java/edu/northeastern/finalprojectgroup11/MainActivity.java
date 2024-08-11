package edu.northeastern.finalprojectgroup11;

import static android.content.ContentValues.TAG;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

//import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
//import java.util.ArrayList;
//import java.util.List;
import java.util.Random;

import android.widget.LinearLayout;
import android.widget.VideoView;

import edu.northeastern.finalprojectgroup11.Music.BGMPlayer;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference userStatusRef;
    private CardView btnLogin;
    private String currentUID;
    private DatabaseReference roomRef;
    private final int maxRoomCount = 2;
    private ValueEventListener player2Join;

    private AlertDialog roomDialog;

    private static final String PUBLIC_ROOM = "PublicRooms";
    private static final String PRIVATE_ROOM = "PrivateRooms";

    private static final String PREFS_NAME = "BGMSettings";
    private static final String KEY_BGM_VOLUME = "bgmVolume";
    private int bgmVolume = 50; // Default volume (50% of max volume)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Background video
//        VideoView videoView = findViewById(R.id.videoView);
//        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.background);
//        videoView.setVideoURI(uri);
//        videoView.start();
//        videoView.setOnPreparedListener(mp -> {
//            mp.setLooping(true);
//            mp.setVolume(0, 0);
//        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPreferences = getSharedPreferences("Battleship", MODE_PRIVATE);

        boolean hasSeenTutorial = sharedPreferences.getBoolean("hasSeenTutorial", false);
        if (!hasSeenTutorial) {
            // First time launch, show the tutorial
            Intent intent = new Intent(this, TutorialActivity.class);
            startActivity(intent);
            finish();
        } else {
            // Proceed with the regular main activity content
            setContentView(R.layout.activity_main);
        }

        currentUID = sharedPreferences.getString("UID", null);

        // Apply the volume to the BGMPlayer
        BGMPlayer.getInstance(this).setVolume(bgmVolume);
        BGMPlayer.getInstance(this).start();

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        // Check if user is logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            signInAnonymously();
        } else {
            currentUID = currentUser.getUid();
            setUserOnlineStatus(currentUID, true);
        }
        btnLogin = findViewById(R.id.login_btn);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkIfGuestAndShowDialog();
            }
        });

        // show diaglog to create and join room
        Button btnPlay = findViewById(R.id.play_friend_btn);
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Inflate the custom layout
                LayoutInflater inflater = getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.dialog_play_friend, null);

                // Create the dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setView(dialogView);
                AlertDialog dialog = builder.create();

                // Set up the buttons
//                Button backButton = dialogView.findViewById(R.id.backButton);
                Button btnCreateRoom = dialogView.findViewById(R.id.button1);
                Button btnJoinRoom = dialogView.findViewById(R.id.button2);

//                backButton.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        dialog.dismiss(); // Dismiss the dialog
//                    }
//                });
                // create room
                btnCreateRoom.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        createRoom();
                        dialog.dismiss();
                    }
                });
                // join room
                btnJoinRoom.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showJoinRoomDialog();
                        dialog.dismiss();
                    }
                });

                // Show the dialog
                dialog.show();
            }
        });

        Button btnPlayOnline = findViewById(R.id.play_online_btn);
        btnPlayOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseReference roomsRef = firebaseDatabase.getReference(PUBLIC_ROOM);

                // Search for a room with only one player (where the game state is "waiting")
                roomsRef.orderByChild("gameState").equalTo("waiting").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        boolean roomFound = false;

                        for (DataSnapshot roomSnapshot : dataSnapshot.getChildren()) {
                            String roomKey = roomSnapshot.getKey();
                            String player2 = roomSnapshot.child("player2").getValue(String.class);

                            // Check if player2 is null, indicating that the room is joinable
                            if (player2 == null) {
                                joinRoomOnline(roomKey);
                                roomFound = true;
                                break;
                            }
                        }

                        // If no joinable room was found, create a new room
                        if (!roomFound) {
                            createRoomOnline();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Handle possible errors
                        Log.e("Firebase", "Error while searching for a room: " + databaseError.getMessage());
                    }
                });
            }
        });

        Button btnBot = findViewById(R.id.test_btn);
        btnBot.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, BotDeployActivity.class);
                startActivity(intent);
            }
        });

        ImageButton btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(v -> showSettingsDialog());
    }

    private void showNetworkErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Network Error")
                .setMessage("You do not have access to the network. Please close the app and reopen it.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
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
                BGMPlayer.getInstance(MainActivity.this).setVolume(volume);

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

//    private void showHowToPlayDialog() {
//        final Dialog howToPlayDialog = new Dialog(this);
//        howToPlayDialog.setContentView(R.layout.dialog_how_to_play);
//        howToPlayDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
//
//        ImageView tutorialImage = howToPlayDialog.findViewById(R.id.tutorial_image);
//        Button btnNext = howToPlayDialog.findViewById(R.id.btn_next);
//        Button btnPrevious = howToPlayDialog.findViewById(R.id.btn_previous);
//
//        // List to hold image URLs or paths
//        List<String> imageUrls = new ArrayList<>();
//        imageUrls.add("url");
//
//
//        final int[] currentIndex = {0};  // Current image index
//
//        // Load the first image
//        loadImage(tutorialImage, imageUrls.get(0));
//
//        btnNext.setOnClickListener(v -> {
//            if (currentIndex[0] < imageUrls.size() - 1) {
//                currentIndex[0]++;
//                loadImage(tutorialImage, imageUrls.get(currentIndex[0]));
//            }
//        });
//
//        btnPrevious.setOnClickListener(v -> {
//            if (currentIndex[0] > 0) {
//                currentIndex[0]--;
//                loadImage(tutorialImage, imageUrls.get(currentIndex[0]));
//            }
//        });
//
//        howToPlayDialog.show();
//    }
//
//    private void loadImage(ImageView imageView, String url) {
//        Glide.with(this)
//                .load(url)
//                .into(imageView);
//    }

    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            currentUID = user.getUid();
                            String guestUsername = "Guest" + new Random().nextInt(1000);
                            setUserOnlineStatus(user.getUid(), true);
                            storeUserData(user.getUid(), guestUsername, true);  // Store user data with isGuest as true
                            sharedPreferences.edit().putString("UID", currentUID).apply();
                        }
                    } else {
                        Log.w(TAG, "signInAnonymously:failure", task.getException());
                        Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void checkIfGuestAndShowDialog() {
        DatabaseReference userRef = firebaseDatabase.getReference("users").child(currentUID);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String username = snapshot.child("username").getValue(String.class);
                Boolean isGuest = snapshot.child("isGuest").getValue(Boolean.class);
                if (username != null) {
                    if (isGuest != null && isGuest) {
                        showGuestDialog(username);
                    } else {
                        showUserOptionsDialog();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showNetworkErrorDialog();
                Log.e(TAG, "Failed to check user status: " + error.getMessage());
            }
        });
    }

    private void showUserOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View optionsView = inflater.inflate(R.layout.dialog_user_options, null);
        builder.setView(optionsView);

        TextView title = optionsView.findViewById(R.id.title);
        EditText usernameEditText = optionsView.findViewById(R.id.username);
        Button btnSaveUsername = optionsView.findViewById(R.id.btnSaveUsername);
        Button btnSignOut = optionsView.findViewById(R.id.btnSignOut);
        TextView statsTextView = optionsView.findViewById(R.id.stats);

        // Fetch the current username and statistics
        DatabaseReference usernameRef = firebaseDatabase.getReference("users")
                .child(currentUID);

        usernameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String currentUsername = snapshot.child("username").getValue(String.class);
                Integer wins = snapshot.child("win").getValue(Integer.class);
                Integer losses = snapshot.child("loss").getValue(Integer.class);
                double winRate = wins + losses > 0 ? (double) wins / (wins + losses) * 100 : 0;
                title.setText("User: " + currentUsername);
                usernameEditText.setHint(currentUsername); // Sets the current username as hint
                statsTextView.setText("Wins: " + wins + " | Losses: " + losses + " | Win Rate: " + String.format("%.2f%%", winRate));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w(TAG, "Failed to read user information.", error.toException());
            }
        });

        AlertDialog dialog = builder.create();

        btnSaveUsername.setOnClickListener(v -> {
            String newUsername = usernameEditText.getText().toString().trim();
            if (!newUsername.isEmpty()) {
                updateUsername(newUsername);
                dialog.dismiss();
            } else {
                Toast.makeText(MainActivity.this, "Username cannot be empty.", Toast.LENGTH_SHORT).show();
            }
        });

        btnSignOut.setOnClickListener(v -> {
            dialog.dismiss();
            showSignOutDialog();
        });

        dialog.show();
    }

    private void updateUsername(String newUsername) {
        DatabaseReference usernameRef = firebaseDatabase.getReference("users")
                .child(currentUID)
                .child("username");

        usernameRef.setValue(newUsername).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(MainActivity.this, "Username updated successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Failed to update username", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showGuestDialog(String username) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Guest Account: " + username)
                .setMessage("You are currently a guest and your game data will not be stored.")
                .setPositiveButton("Login/Register", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showLoginDialog();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showLoginDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View loginView = inflater.inflate(R.layout.dialog_signin, null);
        builder.setView(loginView)
                .setPositiveButton("Login/Register", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText emailEditText = loginView.findViewById(R.id.email);
                        EditText passwordEditText = loginView.findViewById(R.id.password);
                        String email = emailEditText.getText().toString().trim();
                        String password = passwordEditText.getText().toString().trim();

                        if (!isValidEmail(email)) {
                            Toast.makeText(MainActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                        } else if (password.length() < 6) {
                            Toast.makeText(MainActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                        } else {
                            setGuestOffline();
                            signInWithEmail(email, password);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void setGuestOffline() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.isAnonymous()) {
            DatabaseReference guestRef = firebaseDatabase.getReference("users").child(user.getUid()).child("status");
            guestRef.setValue("offline");
        }
    }

    private void signInWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            currentUID = user.getUid();
                            setUserOnlineStatus(user.getUid(), true);
                            sharedPreferences.edit().putString("UID", currentUID).apply();
                            Toast.makeText(MainActivity.this, "Successful login", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if (task.getException() instanceof FirebaseAuthInvalidUserException || task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            // If sign-in fails, attempt to register the user
                            registerWithEmail(email, password);
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void registerWithEmail(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            currentUID = user.getUid();
                            String defaultUsername = "User" + new Random().nextInt(1000);
                            setUserOnlineStatus(user.getUid(), true);
                            storeUserData(user.getUid(), defaultUsername, false); // Store the user data with isGuest as false
                            sharedPreferences.edit().putString("UID", currentUID).apply();
                            Toast.makeText(MainActivity.this, "Successful registration", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            // If registration fails due to an existing user
                            Toast.makeText(MainActivity.this, "Wrong password. Please try again.", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.w(TAG, "registerWithEmail:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Registration failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void storeUserData(String uid, String username, boolean isGuest) {
        DatabaseReference userRef = firebaseDatabase.getReference("users").child(uid);
        userRef.child("username").setValue(username);
        userRef.child("isGuest").setValue(isGuest);
        userRef.child("status").setValue("online");
        userRef.child("win").setValue(0);
        userRef.child("loss").setValue(0);
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
                })
                .setCancelable(false);

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
            Toast.makeText(MainActivity.this, "Signed out successfully", Toast.LENGTH_SHORT).show();
            // Automatically sign in as a guest after signing out
            signInAnonymously();
        } else {
            Log.d(TAG, "No user to sign out");
        }
        Log.d(TAG, "currentUID after sign out: " + currentUID);
        Log.d(TAG, "SharedPreferences UID after sign out: " + sharedPreferences.getString("UID", "null"));
    }



    //create room stuff
    private void createRoom() {
        String roomCode = generateRoomCode();  // Method to generate a random room code

        // check if the newly created room code exist;
        firebaseDatabase.getReference(PRIVATE_ROOM).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // If the server is full
                if (snapshot.getChildrenCount() == maxRoomCount) {
                    Toast.makeText(getApplicationContext(), "Game server is full, try later", Toast.LENGTH_LONG).show();
                }

                // if the room code is unique
                if (!snapshot.hasChild(roomCode)) {
                    // do the create room and show dialog stuff
                    roomRef = firebaseDatabase.getReference(PRIVATE_ROOM).child(roomCode);
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
                                    startGame(roomCode, PRIVATE_ROOM);
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
                    Toast.makeText(getApplicationContext(), "Please try again.", Toast.LENGTH_SHORT).show();
                    //createRoom();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void createRoomOnline() {
        String roomCode = generateRoomCode();  // Method to generate a random room code

        // check if the newly created room code exist;
        firebaseDatabase.getReference(PUBLIC_ROOM).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // If the server is full
                if (snapshot.getChildrenCount() == maxRoomCount) {
                    Toast.makeText(getApplicationContext(), "Game server is full, try later", Toast.LENGTH_LONG).show();
                }

                // if the room code is unique
                if (!snapshot.hasChild(roomCode)) {
                    // do the create room and show dialog stuff
                    roomRef = firebaseDatabase.getReference(PUBLIC_ROOM).child(roomCode);
                    roomRef.child("player1").setValue(currentUID);  // Save the current user's UID as player1
                    roomRef.child("gameState").setValue("waiting"); // Set room state as waiting

                    // If no empty room then user auto-creates a room
                    showRoomOnline();

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
                                    startGame(roomCode, PUBLIC_ROOM);
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
                    Toast.makeText(getApplicationContext(), "Please try again.", Toast.LENGTH_SHORT).show();
                    //createRoom();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private String generateRoomCode() {
        // Get the current time in seconds
        long currentTimeSeconds = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);

        // Use the current time in seconds as a seed to generate a random number
        Random random = new Random(currentTimeSeconds);
        return String.valueOf((int) (random.nextInt(9000) + 1000));  // Generate a four-digit random number
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
                })
                .setCancelable(false);

        roomDialog = builder.create();

        roomDialog.show();
    }

    private void showRoomOnline(){
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_searching, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)  // Set the custom layout as the dialog view
                .setPositiveButton("Quit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        destroyRoom();
                        dialog.cancel();
                    }
                })
                .setCancelable(false);

        roomDialog = builder.create();

        roomDialog.show();
    }

    // Destroy room when player1 quit from create room
    private void destroyRoom() {
        if (player2Join != null) {
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
                })
                .setCancelable(false);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void joinRoom(String roomCode) {
        // use a listener to check if roomcode exist or not
        firebaseDatabase.getReference(PRIVATE_ROOM).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // only join room if room exist
                if (snapshot.hasChild(roomCode)) {
                    roomRef = firebaseDatabase.getReference(PRIVATE_ROOM).child(roomCode);

                    // Check if both players are present to start the game
                    roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.child("player1").exists() && dataSnapshot.child("gameState").getValue().equals("waiting") ) {
                                roomRef.child("player2").setValue(currentUID);  // Save the current user's UID as player2
                                Toast.makeText(getApplicationContext(), "join success, should start game", Toast.LENGTH_SHORT).show();
                                startGame(roomCode, PRIVATE_ROOM);
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

    private void joinRoomOnline(String roomCode){
        firebaseDatabase.getReference(PUBLIC_ROOM).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // only join room if room exist
                if (snapshot.hasChild(roomCode)) {
                    roomRef = firebaseDatabase.getReference(PUBLIC_ROOM).child(roomCode);

                    // Check if both players are present to start the game
                    roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            if (dataSnapshot.child("player1").exists() && dataSnapshot.child("gameState").getValue().equals("waiting") ) {
                                roomRef.child("player2").setValue(currentUID);  // Save the current user's UID as player2
                                Toast.makeText(getApplicationContext(), "join success, should start game", Toast.LENGTH_SHORT).show();
                                startGame(roomCode, PUBLIC_ROOM);
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

    private void startGame(String roomCode, String roomType) {
        BGMPlayer.getInstance(this).stop();
        roomRef.child("gameState").setValue("active"); // Set room state as active

        // Remove listener for player 2 join
        if (player2Join != null){
            roomRef.removeEventListener(player2Join);
        }

        // start deploy activity
        Intent intent = new Intent(MainActivity.this, DeployActivity.class);
        intent.putExtra("roomCode",roomCode); // Pass the room code into new activity
        intent.putExtra("roomType", roomType); // Pass the room type (public/private) into new activity
        startActivity(intent);
    }
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }



    @Override
    protected void onPause() {
        super.onPause();
        BGMPlayer.getInstance(this).pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        BGMPlayer.getInstance(this).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BGMPlayer.getInstance(this).stop();
    }

}