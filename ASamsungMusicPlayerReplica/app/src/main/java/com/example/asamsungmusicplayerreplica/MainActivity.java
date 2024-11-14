package com.example.asamsungmusicplayerreplica;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    /*
     Functions/Tasks:
     1. Container for different fragments which show songs in different grouping
     2. Grap Permission if necessary
     3. Long Song details
    */

    // MusicFile class - store media file
    public static class MusicFile {
        private String path;
        private String title;
        private String artist;
        private String album;
        private String duration;
        private long id;
        private Uri albumArtUri;

        public MusicFile(String path, String title, String artist, String album, String duration, long id, Uri albumArtUri) {
            this.path = path;
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.duration = duration;
            this.id = id;
            this.albumArtUri = albumArtUri;
        }

        // Getters
        public String getDuration() { return duration; }
        public String getAlbum() { return album; }
        public String getArtist() { return artist; }
        public String getTitle() { return title; }
        public String getPath() { return path; }
        public long getId() { return id; }

        // Parcelable implementation
        // For serialzation and passing music file between activities and services
        // https://developer.android.com/reference/android/os/Parcel
        protected MusicFile(Parcel in) {
            path = in.readString();
            title = in.readString();
            artist = in.readString();
            album = in.readString();
            duration = in.readString();
            id = in.readLong();
            albumArtUri = in.readParcelable(Uri.class.getClassLoader());
        }

        public static final Parcelable.Creator<MusicFile> CREATOR = new Parcelable.Creator<MusicFile>() {
            @Override
            public MusicFile createFromParcel(Parcel in) {
                return new MusicFile(in);
            }

            @Override
            public MusicFile[] newArray(int size) {
                return new MusicFile[size];
            }
        };

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(path);
            dest.writeString(title);
            dest.writeString(artist);
            dest.writeString(album);
            dest.writeString(duration);
            dest.writeLong(id);
            dest.writeParcelable(albumArtUri, flags);
        }
    }

    // MusicContentObserver class - update(reload) song list when there is change of content
    private class MusicContentObserver extends ContentObserver {

        public MusicContentObserver(Handler handler) {
            super(handler);
        }
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            loadSongs();
        }
    }

    // ViewPagerAdapter class - for tag layout(screen-slide)
    public static class ViewPagerAdapter extends FragmentStateAdapter {
        private final ArrayList<Fragment> fragments = new ArrayList<>();
        private final ArrayList<String> titles = new ArrayList<>();

        public ViewPagerAdapter(AppCompatActivity activity) {
            super(activity);
        }

        void addFragments(Fragment fragment, String title) {
            fragments.add(fragment);
            titles.add(title);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return fragments.get(position);
        }

        @Override
        public int getItemCount() {
            return fragments.size();
        }
        public String getTitle(int position) {
            return titles.get(position);
        }

    }

    static final int REQUEST_PERMISSION_CODE = 3104;

    static ArrayList<MusicFile> songList; // for Song Fragment
    static ArrayList<MusicFile> albumList; // for Album Fragment
    static HashMap<String, ArrayList<MusicFile>> albumSongsMap;

    private MusicContentObserver musicContentObserver;

    ViewPager2 viewPager;
    TabLayout tabMeunLayout;
    ViewPagerAdapter viewPagerAdapter;

    private SongsFragment songsFragment;
    private AlbumFragment albumFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songsFragment = new SongsFragment();
        albumFragment = new AlbumFragment();

        songList = new ArrayList<>(); // for Song Fragment
        albumList = new ArrayList<>(); // for Album Fragment
        albumSongsMap = new HashMap<>(); // for Album list details

        // ViewPager2 for tag layout(screen-slide)
        // https://developer.android.com/develop/ui/views/animations/screen-slide-2?hl=zh-tw
        viewPager = findViewById(R.id.main_activity_view_pager); // Initialize here
        tabMeunLayout = findViewById(R.id.main_activity_tab_bar_menu_layout);

        if (savedInstanceState != null) {
            int currentFragment = savedInstanceState.getInt("current_fragment");
            viewPager.setCurrentItem(currentFragment);
        }

        // Request permissions based on API ver
        requestPermissionsBasedOnVersion();

        // Register MusicContentObserver to observe changes in the MEDIASTORE
        musicContentObserver = new MusicContentObserver(new Handler());
        getContentResolver().registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                musicContentObserver
        );

    }


    // Helper Functions
    // Request permissions based on API ver
    private void requestPermissionsBasedOnVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33 and above
            if (checkSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                loadSongs();
            } else {
                requestPermissions(new String[]{
                        android.Manifest.permission.READ_MEDIA_IMAGES,
                        android.Manifest.permission.READ_MEDIA_AUDIO
                }, REQUEST_PERMISSION_CODE);
            }
        } else { // API below 30
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            ){
                loadSongs();
            } else {
                requestPermissions(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                }, REQUEST_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Toast.makeText(this, "Permissions Granted", Toast.LENGTH_LONG).show();
                loadSongs();
            } else {
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    // Load songs into the songList and initialize ViewPager
    private void loadSongs() {

        getSongFromMediastore(this);

        if (viewPagerAdapter == null) {
            viewPagerAdapter = new ViewPagerAdapter(this);

            viewPagerAdapter.addFragments(songsFragment, "Songs");
            viewPagerAdapter.addFragments(albumFragment, "Albums");

            viewPager.setAdapter(viewPagerAdapter);
            new TabLayoutMediator(tabMeunLayout, viewPager, (tab, position) -> {
                tab.setText(viewPagerAdapter.getTitle(position));
            }).attach();
        }

        refeshSongList();

    }

    public static void getSongFromMediastore(Context context) {
        Set<String> albumsSet = new HashSet<>();

        // Get Music from MediaStore
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = null;

        try {
            String[] projection = {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA,       // path
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.ALBUM_ID,
            };

            cursor = context.getContentResolver().query(
                    uri, projection, null, null,
                    MediaStore.Audio.Media.DATE_ADDED + " DESC");

            if (cursor != null) {

                while (cursor.moveToNext()) {
                    String musicPath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                    String musicTitle = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                    String musicArtist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                    String musicAlbum = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                    String musicDuration = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                    long albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
                    long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));

                    // Get album art URI with album_id
                    Uri albumArtUri = getAlbumArtUri(context, albumId);

                    MusicFile musicFile = new MusicFile(
                            musicPath,
                            musicTitle,
                            musicArtist,
                            musicAlbum,
                            musicDuration,
                            id,
                            albumArtUri);

                    songList.add(musicFile);

                    if (!albumsSet.contains(musicAlbum)) {
                        albumList.add(musicFile);
                        albumSongsMap.put(musicAlbum, new ArrayList<>());
                        albumsSet.add(musicAlbum);
                    }

                    // Add update to Map
                    ArrayList<MusicFile> currentAlbumSongs = albumSongsMap.get(musicAlbum);
                    if (!currentAlbumSongs.stream().anyMatch(mf -> mf.getId() == musicFile.getId())) {
                        currentAlbumSongs.add(musicFile);
                    }

                }
            }
        } catch (Exception e) {
            Log.e("MainActivity", "getSongFromMediastore() - cannot fetch music", e);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static Uri getAlbumArtUri(Context context, long albumId) {
        Uri albumArtUri = null;
        Cursor albumCursor = null;

        try {
            Uri albumUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
            String[] projection = {MediaStore.Audio.Albums.ALBUM_ART};
            String selection = MediaStore.Audio.Albums._ID + "=?";
            String[] selectionArgs = {String.valueOf(albumId)};

            albumCursor = context.getContentResolver().query(albumUri, projection, selection, selectionArgs, null);

            if (albumCursor != null && albumCursor.moveToFirst()) {
                String albumArtPath = albumCursor.getString(albumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART));
                if (albumArtPath != null) {
                    albumArtUri = Uri.parse(albumArtPath);
                }
            }
        } catch (Exception e) {
            Log.e("MusicPlayer - getAlbumArtUri : ", "Error fetching album art", e);
        } finally {
            if (albumCursor != null) {
                albumCursor.close();
            }
        }
        return albumArtUri;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("current_fragment", viewPager.getCurrentItem());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(musicContentObserver);
    }

    public void refeshSongList(){
        // Update song list
        songsFragment.refreshSongList();
        albumFragment.refreshSongList();

    }
}