package edu.northeastern.finalprojectgroup11.Music;

import android.content.Context;
import android.media.MediaPlayer;

import edu.northeastern.finalprojectgroup11.R;

public class BGMPlayer {
    private static BGMPlayer instance;
    private MediaPlayer mediaPlayer;
    private int currentPosition = 0;

    private BGMPlayer(Context context) {
        mediaPlayer = MediaPlayer.create(context, R.raw.music_main); // Replace with your BGM file
        mediaPlayer.setLooping(true);
    }

    public static BGMPlayer getInstance(Context context) {
        if (instance == null) {
            instance = new BGMPlayer(context);
        }
        return instance;
    }

    public void start() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            mediaPlayer.seekTo(currentPosition); // Resume from the last position
        }
    }

    public void pause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            currentPosition = mediaPlayer.getCurrentPosition(); // Save the current position
        }
    }

    public void stop() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            instance = null; // Reset the instance
        }
    }

    public void setVolume(float volume) {
        mediaPlayer.setVolume(volume, volume);
    }
}

