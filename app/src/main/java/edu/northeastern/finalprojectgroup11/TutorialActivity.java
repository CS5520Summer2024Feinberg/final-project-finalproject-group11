package edu.northeastern.finalprojectgroup11;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.widget.Button;

import edu.northeastern.finalprojectgroup11.Tutorial.TutorialAdapter;

public class TutorialActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private TutorialAdapter tutorialAdapter;
    private Button finishButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        viewPager = findViewById(R.id.viewPager);
        finishButton = findViewById(R.id.finishButton);

        // Set up the tutorial adapter
        tutorialAdapter = new TutorialAdapter(this);
        viewPager.setAdapter(tutorialAdapter);

        // Set up the finish button action
        finishButton.setOnClickListener(v -> {
            // Mark the tutorial as seen in SharedPreferences
            SharedPreferences prefs = getSharedPreferences("Battleship", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("hasSeenTutorial", true);
            editor.apply();

            // Start the MainActivity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();  // Close the TutorialActivity
        });
    }
}
