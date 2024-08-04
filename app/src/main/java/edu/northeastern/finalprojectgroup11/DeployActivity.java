package edu.northeastern.finalprojectgroup11;

import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.List;

public class DeployActivity extends AppCompatActivity {
    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth mAuth;

    private String roomCode;
    private String UID;
    private DatabaseReference roomRef;
    private TextView dragTextView;

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

        // Retrieve room code
        Intent intent = getIntent();
        roomCode = intent.getStringExtra("roomCode");

        // Link to firebase
        firebaseDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        UID = mAuth.getCurrentUser().getUid();
        roomRef = firebaseDatabase.getReference("rooms").child(roomCode);

        // show room code
        TextView roomCodeTextView = findViewById(R.id.textViewRoom);
        roomCodeTextView.setText(roomCode);

        // Set current player as not ready as default
        roomRef.child("players").child(UID).child("playerState").setValue("deploying");

        // Button to set ready
        Button btnReady = findViewById(R.id.buttonReady);
        btnReady.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                roomRef.child("players").child(UID).child("playerState").setValue("ready");
            }
        });

        // Listener to check if both ready and enter battle
        roomRef.child("players").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                checkReady(roomRef);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DeployActivity.this, "Failed to check players' status.", Toast.LENGTH_SHORT).show();
            }
        });

        // Button to quit game
        Button btnQuit = findViewById(R.id.buttonQuit);
        btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                quitGame();
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
                    startActivity(intent);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DeployActivity.this, "Failed to update players' status.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Linked to quit button to surrender
    private void quitGame() {
        if (UID != null && roomRef != null) {
            roomRef.child("players").child(UID).child("playerState").setValue("quit");
        }
        Intent intent = new Intent(DeployActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
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

                    return true;


                case DragEvent.ACTION_DRAG_ENDED:
                    v.setBackgroundColor(getResources().getColor(android.R.color.darker_gray)); // Revert the highlight
                    return true;

                default:
                    break;
            }
            return false;
        }
    }
}
