package com.example.asamsungmusicplayerreplica;

import static com.example.asamsungmusicplayerreplica.ApplicationClass.ACTION_NEXT;
import static com.example.asamsungmusicplayerreplica.ApplicationClass.ACTION_PAUSE;
import static com.example.asamsungmusicplayerreplica.ApplicationClass.ACTION_PLAY;
import static com.example.asamsungmusicplayerreplica.ApplicationClass.ACTION_PREVIOUS;
import static com.example.asamsungmusicplayerreplica.ApplicationClass.CHANNEL_ID_2;
import static com.example.asamsungmusicplayerreplica.MainActivity.songList;
import static com.example.asamsungmusicplayerreplica.MusicPlayer.currentPlayList;
import static com.example.asamsungmusicplayerreplica.MusicPlayer.currentPlayPausebn;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener {


    public static class NotificationReceiver extends BroadcastReceiver {

        // Task:
        // to receiver onclick action from mini player of notification Bar
        // https://developer.android.com/reference/android/content/BroadcastReceiver#onReceive(android.content.Context,%20android.content.Intent)

        @Override
        public void onReceive(Context context, Intent intent) {

            String actionName = intent.getAction();
            Intent serviceIntent = new Intent(context, MusicService.class);

            if(actionName!=null) {
                switch (actionName) {
                    case ACTION_PREVIOUS:
                        serviceIntent.putExtra("ActionType", "previous");
                        context.startService(serviceIntent);
                        break;

                    case ACTION_PAUSE:
                        serviceIntent.putExtra("ActionType", "playPause");
                        context.startService(serviceIntent);
                        break;

                    case ACTION_NEXT:
                        serviceIntent.putExtra("ActionType", "next");
                        context.startService(serviceIntent);
                        break;

                    default:
                        break;
                }
            } else {
                Log.e("NotificationReceiver", "onReceive() - actionName is null");
            }
        }
    }

    // IBinder For IPC
    // https://developer.android.com/reference/android/os/IBinder
    // https://blog.csdn.net/Mr_LiaBill/article/details/49837851
    public class MyBinder extends Binder {
        MusicService getService() { return MusicService.this; }
    }

    IBinder musicServiceBinder = new MyBinder();
    MediaPlayer mediaPlayer;
    ArrayList<MainActivity.MusicFile> servicePlayList =  new ArrayList<>();
    Uri uri;
    ActionOnClick actionOnClick;
    int position;

    // Repeat modes
    public static final int REPEAT_ALL = 0;
    public static final int NO_REPEAT = 1;
    public static final int REPEAT_ONE = 2;
    private int repeatMode = REPEAT_ALL; // repeatAll as default

    // For Media Control and playback
    // https://developer.android.com/media/media3/session/control-playback
    MediaSession mediaSession;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return musicServiceBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaSession = new MediaSession(getBaseContext(), "MusicPlayer");

        // Callback for media control actions
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    start();
                }
            }

            @Override
            public void onPause() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    pause();
                }
            }

            @Override
            public void onSkipToNext() {
                if (actionOnClick != null) {
                    actionOnClick.skipTrackBnClicked(true);
                }
            }

            @Override
            public void onSkipToPrevious() {
                if (actionOnClick != null) {
                    actionOnClick.skipTrackBnClicked(false);
                }
            }
        });
    }

    private void playMedia(Uri currentUri) {
        servicePlayList = currentPlayList;
        uri = currentUri;
        checkNReleaseMusicService();


        if(servicePlayList != null){
            try {
                createMediaPlayer(uri, position);
                mediaPlayer.start();
                mediaPlayer.setOnCompletionListener(this);
            } catch (Exception e) {
                Log.e("MusicService", "playMedia() - Error" + e.getMessage());
            }
        } else {
            Log.w("MusicService", "playMedia() Null Service playlist");
        }

    }

    public void setActionPlaying(MusicPlayer actionOnClick) {
        this.actionOnClick = actionOnClick;
    }

    // Media Player Function
    void createMediaPlayer(Uri uri, int currentPosition){
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        position = currentPosition;
        mediaPlayer = MediaPlayer.create(getBaseContext(), uri);

    }

    void start(){
        if (mediaPlayer != null) {
            mediaPlayer.start();
            MusicPlayer.currentPlayPausebn = R.drawable.bn_pause;
            MusicPlayer.bnPlay.setImageResource(MusicPlayer.currentPlayPausebn);
            showNotification(MusicPlayer.currentPlayPausebn);
        } else {
            Log.e("MusicService", "start() - MediaPlayer null");
        }
    }

    void pause(){
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            MusicPlayer.currentPlayPausebn = R.drawable.bn_play;
            MusicPlayer.bnPlay.setImageResource(MusicPlayer.currentPlayPausebn);
            showNotification(MusicPlayer.currentPlayPausebn);
        } else {
            Log.e("MusicService", "pause() - MediaPlayer null");
        }
    }

    void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            MusicPlayer.currentPlayPausebn = R.drawable.bn_play;
            MusicPlayer.bnPlay.setImageResource(MusicPlayer.currentPlayPausebn);
            showNotification(MusicPlayer.currentPlayPausebn);
        } else {
            Log.e("MusicService", "pause() - MediaPlayer null");
        }
    }

    void seekTo(int position) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        } else {
            Log.e("MusicService", "pause() - MediaPlayer null");
        }
    }

    int getDuration(){  return mediaPlayer != null ? mediaPlayer.getDuration() : 0; }

    int getCurrentPosition(){
        return mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
    }

    boolean isPlaying(){ return mediaPlayer != null && mediaPlayer.isPlaying(); }

    public void setRepeatMode(int mode) {
        repeatMode = mode;
    }

    //  When the song end
    void OnCompleted(){
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(this);
        } else {
            Log.e("MusicService", "OnCompleted() - MediaPlayer is null in OnCompleted");
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

        if (actionOnClick != null) {
            if (repeatMode == REPEAT_ONE) {
                mediaPlayer.seekTo(0); // seek back to beginning
                mediaPlayer.start();
            } else if (repeatMode == NO_REPEAT) {
                if (position == currentPlayList.size() - 1) {
                    int total = Integer.parseInt(currentPlayList.get(position).getDuration())/1000;
                    MusicPlayer.playedDuration.setText(MusicPlayer.formattedDuration(total));
                    pause();

                } else { // just play next if that's not the last song
                    actionOnClick.skipTrackBnClicked(true);
                }
            } else if (repeatMode == REPEAT_ALL) {
                actionOnClick.skipTrackBnClicked(true);
            }
        } else {
            Log.e("MusicService", "onCompletion() - actionPlaying is null");
        }
    }


    // NOTIFICATION BAR
    // mini Player in the notification(Broadcast)
    void showNotification(int noticficationBarPlayPauseBn) {
        Intent conentIntent = new Intent(this, MusicPlayer.class);
        conentIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPending = PendingIntent.getActivity(this, 0, conentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Previous, Play/Pause, Next button intents
        Intent preIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_PREVIOUS);
        PendingIntent prePending = PendingIntent.getBroadcast(this, 1, preIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent pauseIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_PAUSE);
        PendingIntent pausePending = PendingIntent.getBroadcast(this, 2, pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, NotificationReceiver.class).setAction(ACTION_NEXT);
        PendingIntent nextPending = PendingIntent.getBroadcast(this, 3, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        byte[] albumArt = getAlbumArt(currentPlayList.get(position).getPath());
        Bitmap thumb = (albumArt != null) ? BitmapFactory.decodeByteArray(albumArt, 0, albumArt.length)
                : BitmapFactory.decodeResource(getResources(), R.drawable.default_ablum_art);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID_2)
                .setSmallIcon(noticficationBarPlayPauseBn)
                .setLargeIcon(thumb)
                .setContentTitle(servicePlayList.get(position).getTitle())
                .setContentText(servicePlayList.get(position).getArtist())

                .setContentIntent(contentPending)
                .addAction(R.drawable.bn_skip_previous, "Previous", prePending)
                .addAction(noticficationBarPlayPauseBn, "Play", pausePending)
                .addAction(R.drawable.bn_next, "Next", nextPending)

                .setAutoCancel(true)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(MediaSessionCompat.Token.fromToken(mediaSession.getSessionToken())))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();

        startForeground(2, notification);
    }

    // get album art from uri
    // https://developer.android.com/reference/kotlin/android/media/MediaMetadataRetriever#getEmbeddedPicture()
    private byte[] getAlbumArt(String uri) {

        if (uri == null) {
            Log.e("MusicService", "getAlbumArt() - null URI");
            return null;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(uri);
            return retriever.getEmbeddedPicture();
        } catch (IllegalArgumentException e) {
            Log.e("MusicService", "getAlbumArt() - Invalid URI: " + uri, e);
            return null;
        } catch (IllegalStateException e) {
            Log.e("MusicService", "getAlbumArt() -  invalid state.", e);
            return null;
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException e) {
                Log.e("MusicService", "getAlbumArt() - Error on releasing MediaMetadataRetriever", e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent serviceIntent = new Intent(this, MusicService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        // Extract current URI and action type from intent
        Uri currentUri = intent.getParcelableExtra("currentUri");
        String actionType = intent.getStringExtra("ActionType");
        position = intent.getIntExtra("servicePosition", position);

        handleNotificationAction(actionType);


        if (currentUri != null) {
            playMedia(currentUri);
        }

        handleNotificationAction(ACTION_PLAY);
        return START_STICKY;

    }

    private void handleNotificationAction(String actionType) {
        if(actionType!=null){
            switch (actionType) {
                case "previous":
                    if(actionOnClick != null) actionOnClick.skipTrackBnClicked(false);
                    break;

                case "playPause":
                    if(actionOnClick != null) actionOnClick.playBnClicked();
                    break;

                case "next":
                    if(actionOnClick != null) actionOnClick.skipTrackBnClicked(true);
                    break;
            }
        } else {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) actionType = ACTION_PLAY;
        }
    }

    void killMusicService(){
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release(); // Release resources
            } catch (Exception e) {
                Log.e("MusicService", "killMusicService() - Error " + e.getMessage());
            } finally {
                mediaPlayer = null;
            }
        }
        //stopSelf();
    }
    void checkNReleaseMusicService(){
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release(); // Release resources
            } catch (Exception e) {
                Log.e("MediaPlayback", "checkNReleaseMusicService()  - Error" + e.getMessage());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }

        killMusicService();
    }


}
