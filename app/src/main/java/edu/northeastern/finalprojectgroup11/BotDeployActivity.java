package edu.northeastern.finalprojectgroup11;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.gridlayout.widget.GridLayout;


// need add quit button, reset button, random place button, count down, UI: timer and mine icon, ready change color when hit link quit with back
public class BotDeployActivity extends AppCompatActivity {
private GameBoard board;
private GridLayout gridLayout;
private int rows = 10;
private int cols = 10;

private TextView mineLeftextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bot_deploy);
        mineLeftextView = findViewById(R.id.mineLeftTextView);

        // Ready button to start game
        Button btnReady = findViewById(R.id.buttonReady);
        btnReady.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (board.isDeployReady()) {
                    Intent intent = new Intent(BotDeployActivity.this, BotBattleActivity.class);
                    GameBoardManager.setGameBoard(board);
                    startActivity(intent);
                } else {
                    Toast.makeText(BotDeployActivity.this, "Deploy all the mines before start.", Toast.LENGTH_SHORT).show();
                }
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

                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(i, 1f), GridLayout.spec(j, 1f));
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
    }

    public void onCellClick(int row, int col) {
        Button button = (Button) gridLayout.getChildAt(row * board.getCols() + col);

        if (!board.hasMine(row, col) && board.getMineToPlace() > 0) {
            board.placeMine(row, col);

            // Update the view to show the mine placement
            button.setText("\u26AB");
            button.setGravity(Gravity.CENTER);
            button.setTextSize(20);
            button.setPadding(5, 5, 5, 5);


        } else {
            board.removeMineDeploy(row,col);

            button.setText(""); // Mark the mine
        }

        mineLeftextView.setText(String.valueOf(board.getMineToPlace()));
    }
}




