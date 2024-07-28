package edu.northeastern.finalprojectgroup11;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

public class DeployActivity extends AppCompatActivity {
    private FirebaseDatabase firebaseDatabase;
    private FirebaseAuth mAuth;

    private String roomCode;
    private String UID;

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

        // show room code
        TextView roomCodeTextView = findViewById(R.id.textViewRoom);
        roomCodeTextView.setText(roomCode);

        // Set current player as not ready as defaut
        DatabaseReference roomRef = firebaseDatabase.getReference("rooms").child(roomCode);
        roomRef.child("players").child(UID).child("Ready").setValue(false);

        // Button to set ready
        Button btnReady = findViewById(R.id.buttonReady);
        btnReady.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                roomRef.child("players").child(UID).child("Ready").setValue(true);
            }
        });

        // Listner to check if both ready and enter battle
        roomRef.child("players").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //checkReady(roomRef);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DeployActivity.this, "Failed to check players' status.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Check if both player are ready and start game activity if ready
    private void checkReady(DatabaseReference roomRef) {
        roomRef.child("players").addListenerForSingleValueEvent(new ValueEventListener() {
            boolean allReady = true;
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot playerSnapshot:snapshot.getChildren()){
                    Boolean ready = playerSnapshot.child("ready").getValue(Boolean.class);
                    if (!ready || ready == null) {
                        allReady = false;
                        break;
                    }
                }

                if (allReady) {
                    Toast.makeText(DeployActivity.this, "game should start.", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DeployActivity.this, "Failed to update players' status.", Toast.LENGTH_SHORT).show();
            }
        });

    }

}