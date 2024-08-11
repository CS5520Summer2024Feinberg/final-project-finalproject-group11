package edu.northeastern.finalprojectgroup11;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.gridlayout.widget.GridLayout;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import edu.northeastern.finalprojectgroup11.Music.BGMPlayer;
import edu.northeastern.finalprojectgroup11.Music.BGMPlayer2;


//  UI: timer and mine icon, ready change color when hit link quit with back
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

private static final String PREFS_NAME = "BGMSettings";
private static final String KEY_BGM_VOLUME = "bgmVolume";
private int bgmVolume = 50; // Default volume (50% of max volume)


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bot_deploy);

        // Apply the volume to the BGMPlayer
        BGMPlayer2.getInstance(this).setVolume(bgmVolume);
        BGMPlayer2.getInstance(this).start();

        handler = new Handler(Looper.getMainLooper()); // for delay

        mineLeftextView = findViewById(R.id.mineLeftTextView);
        countdownTextView = findViewById(R.id.countdownTextView);
        Button btnReady = findViewById(R.id.buttonReady);
        Button btnRandom = findViewById(R.id.buttonRandom);
        Button btnReset = findViewById(R.id.buttonReset);
        Button btnQuit = findViewById(R.id.buttonQuit);

        ImageButton btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(v -> showSettingsDialog());

        // Initialize the countdown timer for 10 seconds
        countDownTimer = new CountDownTimer(20000, 1000) {
            public void onTick(long millisUntilFinished) {
                // Update the countdown text each second
                countdownTextView.setText("" + millisUntilFinished / 1000);
            }

            public void onFinish() {
                placeRandom();
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
                }, 1500);

            }
        };
        // Start the countdown when the activity is created
        countDownTimer.start();


        // Ready button to start game
        btnReady.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onReadyClick();
            }
        });


        // Random button to random place mine
        btnRandom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onResetClick();
                placeRandom();
            }
        });

        // Reset button to clear the mine on the board
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onResetClick();
            }
        });

        // Quit button
        btnQuit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showQuitConfirmationDialog();
            }
        });

        // Link back button with quit
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showQuitConfirmationDialog(); // Show the same quit confirmation dialog
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);

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
                BGMPlayer2.getInstance(BotDeployActivity.this).setVolume(volume);

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
            board.removeMine(row,col);
            button.setText(""); // Mark the mine
            mineLeftextView.setText(String.valueOf(board.getMineToPlace()));
            return -1;
        }
    }


    // Action when the ready button is clicked
    public void onReadyClick() {
        if (board.isDeployReady()) {
            // Cancel the timer
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }

            Intent intent = new Intent(BotDeployActivity.this, BotBattleActivity.class);
            GameBoardManager.setGameBoard(board);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(BotDeployActivity.this, "Deploy all the mines before start.", Toast.LENGTH_SHORT).show();
        }
    }


    // Reset the board by click cell with mine
    public void onResetClick() {
        for (int i=0; i<rows; i++) {
            for (int j=0; j<cols; j++) {
                if (board.hasMine(i,j)) {
                    onCellClick(i,j);
                }
            }
        }
    }


    // Randomly place mines
    public void placeRandom() {
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
    }


    private void setGridLayoutEnabled(boolean enabled) {
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            View child = gridLayout.getChildAt(i);
            child.setEnabled(enabled);
        }
    }

    private void showQuitConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Surrender")
                .setMessage("Are you sure you want to surrender?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showLoseDialog();
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

    private void navigateToMain() {
        Intent intent = new Intent(BotDeployActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        BGMPlayer2.getInstance(this).stop();
        finish();
    }


}




