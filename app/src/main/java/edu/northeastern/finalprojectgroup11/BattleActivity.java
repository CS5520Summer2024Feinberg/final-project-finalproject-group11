package edu.northeastern.finalprojectgroup11;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.GridLayout;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class BattleActivity extends AppCompatActivity {
    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth mAuth;
    private DatabaseReference roomRef;
    private String UID;
    private String OppoentUID;
    private String roomCode;
    private int boat1row;
    private int boat1col;
    private int playerPosition;
    private int currentTurn;


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
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // Get UID of the opponent
                if (Objects.equals(snapshot.child("player1").getValue(String.class), UID)) {
                    playerPosition = 1;
                    OppoentUID = snapshot.child("player2").getValue(String.class);
                } else {
                    playerPosition = 2;
                    OppoentUID = snapshot.child("player1").getValue(String.class);
                }

                Log.d(TAG, "Opponent UID: " + OppoentUID);

                // Get opponent boat location
                boat1row = snapshot.child("players").child(OppoentUID).child("boat1Location").child("row").getValue(Integer.class);
                boat1col = snapshot.child("players").child(OppoentUID).child("boat1Location").child("col").getValue(Integer.class);

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        });

        // determining who's turn
        roomRef.child("turn").setValue(1);
        roomRef.child("turn").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentTurn = snapshot.getValue(Integer.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // grid layout stuff
        GridLayout opponentGrid = findViewById(R.id.opponentGrid);
        for (int i = 0; i < opponentGrid.getChildCount(); i++) {
            View block = opponentGrid.getChildAt(i);
            int row = i / opponentGrid.getColumnCount();
            int col = i % opponentGrid.getRowCount();
            block.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // Current player select a block to hit
                    if (currentTurn == playerPosition) {
                        if (row == boat1row && col == boat1col) {
                            v.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
                            roomRef.child("turn").setValue(1+(currentTurn+2)%2);
                        } else {
                            v.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
                            roomRef.child("turn").setValue(1+(currentTurn+2)%2);
                        }
                    }

                }
            });
        }


    }
}