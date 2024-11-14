package com.example.asamsungmusicplayerreplica;

import static com.example.asamsungmusicplayerreplica.AlbumDetailsActivity.AlbumDetailsAdapter.albumSongList;
import static com.example.asamsungmusicplayerreplica.MainActivity.songList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.palette.graphics.Palette;

import java.util.ArrayList;
import java.util.Random;

public class MusicPlayer extends AppCompatActivity implements ActionOnClick, ServiceConnection {

    TextView songName;
    TextView artistName;
    static TextView playedDuration;
    TextView totalDuration;
    ImageView albumArt, bnShuffle, bnPrevious, bnNext, bnRepeat;
    static ImageView bnPlay;
    static SeekBar musicSeekBar;
    boolean isRepeated = true; // default repeat all
    boolean isShuffled = false;

    // For Repeat Mode
    private static final int REPEAT_ALL = 0;
    private static final int NO_REPEAT = 1;
    private static final int REPEAT_ONE = 2;
    private int repeatMode = REPEAT_ALL; // default to repeat all



    int position = -1; // to track current position
    static ArrayList<MainActivity.MusicFile> currentPlayList = new ArrayList<>();
    static Uri uri;

    Handler handler = new Handler();
    MusicService musicService;

    // for save state
    private String currentSongPath;
    private boolean isPlaying;

    public static int currentPlayPausebn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFullScreen();
        setContentView(R.layout.activity_music_player);

        if (getSupportActionBar() != null) getSupportActionBar().hide();    // Hide action bar

        initContent();
        getIntentMethod();

        // Restore state
        if (savedInstanceState != null) {
            currentSongPath = savedInstanceState.getString("currentSongPath");
            isPlaying = savedInstanceState.getBoolean("isPlaying");
            for (int i = 0; i < currentPlayList.size(); i++) {
                if (currentPlayList.get(i).getPath().equals(currentSongPath)) {
                    position = i;
                    break;
                }
            }

            if (isPlaying) playSong(currentSongPath);
        }

        setupSeekBar();
        setupButtons();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicService.MyBinder musicBinder = (MusicService.MyBinder) service;
        musicService = musicBinder.getService();
        musicService.setActionPlaying(MusicPlayer.this);
        musicSeekBar.setMax(musicService.getDuration()/1000);
        metaData(uri);
        songName.setText(currentPlayList.get(position).getTitle());
        artistName.setText(currentPlayList.get(position).getArtist());
        currentPlayPausebn = R.drawable.bn_pause;
        musicService.showNotification(currentPlayPausebn);
        musicService.OnCompleted();
    }

    @Override
    public void playBnClicked() {

        if (musicService != null) {
            if (musicService.isPlaying()) { // pause if isPlaying
                musicService.pause();
            } else { // play if isPaused
                musicService.start();
            }
        }

        musicSeekBar.setMax(musicService.getDuration() / 1000);
        updateSeekBar.run();
    }

    @Override
    public void skipTrackBnClicked(boolean isNext) {
        musicService.checkNReleaseMusicService();

        // get the next song position
        if (isShuffled) position=getRandomNumber(currentPlayList.size()-1);
        else if (isNext) position = (position + 1) % currentPlayList.size();  // next
        else position = (position - 1 < 0) ? (currentPlayList.size() - 1) : (position - 1);  // previous

        uri = Uri.parse(currentPlayList.get(position).getPath());
        musicService.createMediaPlayer(uri, position);
        metaData(uri);  // Update metadata like album art, title, etc.

        songName.setText(currentPlayList.get(position).getTitle());
        artistName.setText(currentPlayList.get(position).getArtist());

        musicSeekBar.setMax(musicService.getDuration() / 1000);
        updateSeekBar.run();

        musicService.OnCompleted();
        musicService.start();
    }

    private void playSong(String songPath) {
        musicService.checkNReleaseMusicService();
        currentSongPath = songPath; // Save current song path foe rotation etc
        uri = Uri.parse(songPath);
        musicService.createMediaPlayer(uri, position);
        if (musicService != null) {
            musicService.start();
            musicService.OnCompleted();

            // Update UI
            songName.setText(currentPlayList.get(position).getTitle());
            artistName.setText(currentPlayList.get(position).getArtist());
            musicSeekBar.setMax(musicService.getDuration() / 1000);
            updateSeekBar.run();
        } else {
            Log.e("MusicPlayer", "playSong() - null musicService");
        }
    }

    // Init setup
    private void initContent() {

        songName = findViewById(R.id.music_player_activity_player_song_name);
        artistName = findViewById(R.id.music_player_activity_player_artist);
        playedDuration = findViewById(R.id.music_player_activity_player_played_duration);
        totalDuration = findViewById(R.id.music_player_activity_player_duration);

        albumArt = findViewById(R.id.music_player_activity_player_album_art);
        bnShuffle = findViewById(R.id.bn_shuffle);
        bnPrevious = findViewById(R.id.bn_previous);
        bnNext = findViewById(R.id.bn_next);
        bnRepeat = findViewById(R.id.bn_repeat);
        bnPlay = findViewById(R.id.bn_play);

        musicSeekBar = findViewById(R.id.music_player_activity_player_music_seekBar);
    }

    private void getIntentMethod() {
        position = getIntent().getIntExtra("position", -1);
        String sender = getIntent().getStringExtra("sender");

        if (sender!= null) {
            if (sender.equals("albumDetailsAdapter")) currentPlayList = albumSongList;
            else if (sender.equals("songAdapter")) currentPlayList = songList;
        }

        if (currentPlayList != null && !currentPlayList.isEmpty() && position >= 0 && position < currentPlayList.size()) {
            bnPlay.setImageResource(R.drawable.bn_pause);
            uri = Uri.parse(currentPlayList.get(position).getPath());

            Intent intent = new Intent(this, MusicService.class);
            intent.putExtra("servicePosition", position);
            intent.putExtra("currentUri", uri);
            startService(intent);
        } else {
            Log.e("MusicPlayer",
                    "getIntentMethod() - Invalid position or empty playlist. Position: " + position + ", " +
                            "PlayList size: " + (currentPlayList != null ? currentPlayList.size() : 0));
        }
    }

    private void setupSeekBar() {
        musicSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (musicService != null && fromUser) musicService.seekTo(progress * 1000);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                handler.post(updateSeekBar);
            }
        });
        updateSeekBar.run();
    }

    private void setupButtons() {
        bnShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isShuffled = !isShuffled;
                bnShuffle.setImageResource(isShuffled ? R.drawable.bn_shuffle_on : R.drawable.bn_shuffle_off);
            }
        });

        bnRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isRepeated = !isRepeated;
                bnRepeat.setImageResource(isRepeated ? R.drawable.bn_repeat_on : R.drawable.bn_repeat_off);
            }
        });

        bnRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // loop within the repeat modes
                if (repeatMode == REPEAT_ALL) {
                    repeatMode = NO_REPEAT;
                } else if (repeatMode == NO_REPEAT) {
                    repeatMode = REPEAT_ONE;
                } else {
                    repeatMode = REPEAT_ALL;
                }
                updateRepeatButtonIcon();
            }
        });

        bnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playBnClicked();
            }
        });

        bnPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipTrackBnClicked(false);
            }
        });

        bnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                skipTrackBnClicked(true);
            }
        });

    }

    // Helper Fuction
    static String formattedDuration(int currentPosition) {
        String tempString1 = "";
        String tempString2 = "";
        String s = String.valueOf(currentPosition%60);
        String m = String.valueOf(currentPosition/60);
        tempString1 = m + ":" + s;
        tempString2 = m + ":" + "0" + s;
        if(s.length() == 1) return tempString2;
        else return tempString1;
    }

    private int getRandomNumber(int size){
        if (size <= 0) return 0;
        Random random = new Random();
        return random.nextInt(size+1);
    }

    // UI related
    private void setFullScreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE); // remove title bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    private void metaData(Uri uri) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(uri.toString());

        int total = Integer.parseInt(currentPlayList.get(position).getDuration())/1000;
        totalDuration.setText(formattedDuration(total));

        byte[] albumArtBytes = retriever.getEmbeddedPicture();
        Bitmap albumArtBitmap = null;

        if (albumArtBytes != null) {
            albumArtBitmap = BitmapFactory.decodeByteArray(albumArtBytes, 0, albumArtBytes.length);
            ImageAnimation(this, albumArt, albumArtBitmap);
            albumArt.setImageBitmap(albumArtBitmap); // Set Bitmap to the AlbumArt
            // syny BG and Text colour with Album Art
            // https://developer.android.com/develop/ui/views/graphics/palette-colors?hl=zh-tw#groovy
            updateUIWithPalette(albumArtBitmap);
        }
        else {
            albumArt.setImageResource(R.drawable.default_ablum_art);
            albumArtBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_ablum_art);
            ImageAnimation(this, albumArt, albumArtBitmap);
            updateUIWithPalette(albumArtBitmap);
        }
    }

    // AblumArt & Colour related
    // https://developer.android.com/develop/ui/views/graphics/palette-colors?hl=zh-tw
    private void updateUIWithPalette(Bitmap albumArtBitmap) {
        Palette.from(albumArtBitmap).generate(new Palette.PaletteAsyncListener() {
            @Override
            public void onGenerated(@Nullable Palette palette) {
                assert palette != null;
                Palette.Swatch swatch = palette.getDominantSwatch();
                if (swatch != null) {
                    setUIColors(swatch.getRgb(), swatch.getTitleTextColor(), swatch.getBodyTextColor());
                } else {
                    int transparentColor = 0x80000000; // 50% transparent black
                    setUIColors(transparentColor, R.color.textWhite, R.color.textWhite);
                }
            }
        });
    }

    private void setUIColors(int gradientColor, int titleColor, int bodyColor) {
        ImageView gredient = findViewById(R.id.music_player_activity_player_album_art_gredient);
        RelativeLayout playerContainer = findViewById(R.id.music_player_activity_Container_relativelayout);

        GradientDrawable gradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{gradientColor, 0x00000000});
        gredient.setBackground(gradientDrawable);

        GradientDrawable gradientDrawableBg = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{gradientColor, gradientColor});
        playerContainer.setBackground(gradientDrawableBg);

        songName.setTextColor(titleColor);
        artistName.setTextColor(bodyColor);
    }

    // https://developer.android.com/reference/android/view/animation/AnimationUtils
    public void ImageAnimation (Context playerContext, ImageView imageview, Bitmap bitmap){

        Animation animationIn = AnimationUtils.loadAnimation(playerContext, android.R.anim.fade_in);
        Animation animationOut = AnimationUtils.loadAnimation(playerContext, android.R.anim.fade_out);

        animationOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                imageview.setImageBitmap(bitmap);
                animationIn.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                imageview.startAnimation(animationIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        imageview.startAnimation(animationOut);
    }


    // Update related
    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (musicService != null) {
                int currentPosition = musicService.getCurrentPosition() / 1000;
                musicSeekBar.setProgress(currentPosition);
                playedDuration.setText(formattedDuration(currentPosition));
            }
            handler.postDelayed(this, 1000);
        }
    };


    private void updateRepeatButtonIcon() {
        if (repeatMode == REPEAT_ALL) {
            bnRepeat.setImageResource(R.drawable.bn_repeat_on);  // Repeat All
            musicService.setRepeatMode(REPEAT_ALL);
        } else if (repeatMode == NO_REPEAT) {
            bnRepeat.setImageResource(R.drawable.bn_repeat_off);  // No Repeat
            musicService.setRepeatMode(NO_REPEAT);
        } else if (repeatMode == REPEAT_ONE) {
            bnRepeat.setImageResource(R.drawable.bn_repeat_one_on);  // Repeat One
            musicService.setRepeatMode(REPEAT_ONE);
        }
    }

    // call after onStart() before onResume(), only call if the activity was previously destroyed
    // -  restore UI elements
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        int currentPosition = savedInstanceState.getInt("currentPosition", 0);
        boolean isPlaying = savedInstanceState.getBoolean("isPlaying", false);

        musicService.seekTo(currentPosition);
        if (isPlaying) musicService.start();

    }

    // call when comes into the foreground, call after onStart() & everytime resume visible
    // - Resumes or reinitializes tasks that were paused when the activity was not visible.
    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, this, BIND_AUTO_CREATE);
        setupButtons();
        if (musicService != null) {
            musicSeekBar.setMax(musicService.getDuration() / 1000);
            updateSeekBar.run();
        }
    }

    // save or exit app
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentSongPath", currentSongPath);
        outState.putInt("currentPosition", musicService.getCurrentPosition());
        outState.putBoolean("isPlaying", (musicService != null && musicService.isPlaying()));
    }

    @Override
    public void onServiceDisconnected(ComponentName name) { musicService.killMusicService(); }

    @Override
    protected void onPause() {
        super.onPause();
        Intent intent = new Intent(this, MusicService.class);
        intent.putExtra("savedUri", uri);
        intent.putExtra("savedPosition", position);
        startService(intent);
        handler.removeCallbacks(updateSeekBar); // Stop updating SeekBar on pause
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        musicService.killMusicService();
        handler.removeCallbacks(updateSeekBar); // Stop updating SeekBar on destroy
    }

}