package edu.northeastern.finalprojectgroup11;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
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

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


// need add quit button, reset button, random place button, count down, UI: timer and mine icon, ready change color when hit link quit with back
public class BotDeployActivity extends AppCompatActivity {
private GameBoard board;
private GridLayout gridLayout;
private int rows = 10;
private int cols = 10;
private Random random = new Random();
private Handler handler; // used for delay

private CountDownTimer countDownTimer;
private TextView countdownTextView; // TextView to show the countdown


private TextView mineLeftextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bot_deploy);
        mineLeftextView = findViewById(R.id.mineLeftTextView);
        countdownTextView = findViewById(R.id.countdownTextView);
        handler = new Handler(Looper.getMainLooper()); // for delay


        // Initialize the countdown timer for 10 seconds
        countDownTimer = new CountDownTimer(5000, 1000) {
            public void onTick(long millisUntilFinished) {
                // Update the countdown text each second
                countdownTextView.setText("" + millisUntilFinished / 1000);
            }

            public void onFinish() {
                // Randomly place mines on the current board
                int minesToPlace = board.getMineToPlace();
                while (minesToPlace > 0) {
                    int row = random.nextInt(rows);
                    int col = random.nextInt(cols);
                    if (onCellClick(row, col) == 1) {
                        minesToPlace--;
                    } else {
                        minesToPlace++;
                    }
                }
                Toast.makeText(BotDeployActivity.this, "Time's up! Random move selected.", Toast.LENGTH_SHORT).show();
                // Prevent user click after random select
                setGridLayoutEnabled(false);

                // Add delay
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setGridLayoutEnabled(true); // reset to true
                        onReadyClick();
                    }
                }, 1000);

            }
        };

        // Start the countdown when the activity is created
        countDownTimer.start();


        // Ready button to start game
        Button btnReady = findViewById(R.id.buttonReady);
        btnReady.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onReadyClick();
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

    // 1 mine is placed, -1 mine is removed
    public int onCellClick(int row, int col) {
        Button button = (Button) gridLayout.getChildAt(row * board.getCols() + col);

        if (!board.hasMine(row, col) && board.getMineToPlace() > 0) {
            board.placeMine(row, col);

            // Update the view to show the mine placement
            button.setText("\u26AB");
            button.setGravity(Gravity.CENTER);
            button.setTextSize(20);
            button.setPadding(5, 5, 5, 5);
            mineLeftextView.setText(String.valueOf(board.getMineToPlace()));

            return 1;

        } else {
            board.removeMineDeploy(row,col);
            button.setText(""); // Mark the mine
            mineLeftextView.setText(String.valueOf(board.getMineToPlace()));
            return -1;
        }
    }

    // Action when the ready button is clicked
    public void onReadyClick() {
        if (board.isDeployReady()) {
            Intent intent = new Intent(BotDeployActivity.this, BotBattleActivity.class);
            GameBoardManager.setGameBoard(board);
            startActivity(intent);
        } else {
            Toast.makeText(BotDeployActivity.this, "Deploy all the mines before start.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setGridLayoutEnabled(boolean enabled) {
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View child = gridLayout.getChildAt(i);
            child.setEnabled(enabled);
        }
    }

}




