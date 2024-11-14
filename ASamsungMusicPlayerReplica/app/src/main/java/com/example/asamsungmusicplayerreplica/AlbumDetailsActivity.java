package com.example.asamsungmusicplayerreplica;

import static com.example.asamsungmusicplayerreplica.MainActivity.albumSongsMap;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;

public class AlbumDetailsActivity extends AppCompatActivity {

    public static class AlbumDetailsAdapter extends RecyclerView.Adapter<AlbumDetailsAdapter.AlbumDetailsViewHolder>{
        private Context albumDetailsContext;
        static ArrayList<MainActivity.MusicFile> albumSongList;
        View albumDetailsView;

        public class AlbumDetailsViewHolder extends RecyclerView.ViewHolder{
            ImageView albumArt;
            TextView songName;
            TextView artistName;

            public AlbumDetailsViewHolder(View itemView){
                super(itemView);
                albumArt = itemView.findViewById(R.id.music_item_album_art);
                songName = itemView.findViewById(R.id.music_itme_song_name);
                artistName = itemView.findViewById(R.id.music_itme_artist);

            }
        }

        public AlbumDetailsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            albumDetailsView = LayoutInflater.from(albumDetailsContext).inflate(R.layout.music_item, parent, false);
            return  new AlbumDetailsViewHolder(albumDetailsView);
        }

        public AlbumDetailsAdapter(Context albumDetailsContext, ArrayList<MainActivity.MusicFile> albumSongList) {
            this.albumDetailsContext = albumDetailsContext;
            this.albumSongList = albumSongList;
        }

        @Override
        public void onBindViewHolder(@NonNull AlbumDetailsViewHolder holder, @SuppressLint("RecyclerView") int position) {

            MainActivity.MusicFile musicFile = albumSongList.get(position);

            // Set song details
            holder.songName.setText(musicFile.getTitle());
            holder.songName.setSelected(true);
            holder.artistName.setText(musicFile.getArtist());
            holder.artistName.setSelected(true);

            byte[] image = null;
            if (musicFile.getPath() != null) {
                image = getAlbumArt(musicFile.getPath());
            }
            if (image != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
                holder.albumArt.setImageBitmap(bitmap);
            } else {
                holder.albumArt.setImageResource(R.drawable.default_ablum_art);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(albumDetailsContext, MusicPlayer.class);
                intent.putExtra("sender", "albumDetailsAdapter");
                intent.putExtra("position", position);
                albumDetailsContext.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return albumSongList.size();
        }

        // get album art from uri
        // https://developer.android.com/reference/kotlin/android/media/MediaMetadataRetriever#getEmbeddedPicture()
        private byte[] getAlbumArt(String uri) {

            if (uri == null) {
                Log.e("AlbumDetailsAdapter", "getAlbumArt() - null URI");
                return null;
            }

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(uri);
                return retriever.getEmbeddedPicture();
            } catch (IllegalArgumentException e) {
                Log.e("AlbumDetailsAdapter", "getAlbumArt() - Invalid URI: " + uri, e);
                return null;
            } catch (IllegalStateException e) {
                Log.e("AlbumDetailsAdapter", "getAlbumArt() -  invalid state.", e);
                return null;
            } finally {
                try {
                    retriever.release();
                } catch (RuntimeException e) {
                    Log.e("AlbumDetailsAdapter", "getAlbumArt() - Error on releasing MediaMetadataRetriever", e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }



    RecyclerView recyclerView;
    ImageView albumArt;
    String albumName, artistName;
    TextView noSongsText;

    AlbumDetailsAdapter albumDetailsAdapter;
    ArrayList<MainActivity.MusicFile> albumSongsList = new ArrayList<>();

    public class MediaContentObserver extends ContentObserver {

        private Context context;

        public MediaContentObserver(Handler handler, Context context) {
            super(handler);
            this.context = context;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            refreshSongList();
        }
    }

    private MediaContentObserver mediaContentObserver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album_details);

        Handler handler = new Handler();
        mediaContentObserver = new MediaContentObserver(handler, this);

        getContentResolver().registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true, mediaContentObserver);

        recyclerView = findViewById(R.id.album_details_activity_album_details_items_recyclerview);
        albumArt = findViewById(R.id.album_details_activity_album_details_ablum_art);

        albumName = getIntent().getStringExtra("albumName");
        albumSongsList = albumSongsMap.get(albumName);

        setupUI();

    }


    private void setupUI() {
        noSongsText = findViewById(R.id.albumDetails_album_list_no_songs_text);

        if (albumSongsList != null && !albumSongsList.isEmpty()) {
            byte[] image = getAlbumArt(albumSongsList.get(0).getPath());
            if (image != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
                albumArt.setImageBitmap(bitmap);
            } else {
                albumArt.setImageResource(R.drawable.default_ablum_art);
            }

            albumDetailsAdapter = new AlbumDetailsAdapter(this, albumSongsList);
            recyclerView.setAdapter(albumDetailsAdapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));

            noSongsText.setVisibility(View.GONE);  // Hide No song message
            recyclerView.setVisibility(View.VISIBLE);  // Show Song List

        } else {
            albumArt.setImageResource(R.drawable.default_ablum_art);
            recyclerView.setVisibility(View.GONE);  // Show Song List
            noSongsText.setVisibility(View.VISIBLE);  // Show No song message
        }
    }

    // get album art from uri
    // https://developer.android.com/reference/kotlin/android/media/MediaMetadataRetriever#getEmbeddedPicture()
    private byte[] getAlbumArt(String uri) {

        if (uri == null) {
            Log.e("AlbumDetailsAdapter", "getAlbumArt() - null URI");
            return null;
        }

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(uri);
            return retriever.getEmbeddedPicture();
        } catch (IllegalArgumentException e) {
            Log.e("AlbumDetailsAdapter", "getAlbumArt() - Invalid URI: " + uri, e);
            return null;
        } catch (IllegalStateException e) {
            Log.e("AlbumDetailsAdapter", "getAlbumArt() -  invalid state.", e);
            return null;
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException e) {
                Log.e("AlbumDetailsAdapter", "getAlbumArt() - Error on releasing MediaMetadataRetriever", e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Helper Functions
    // refresh list when Mediastore content change
    public void refreshSongList() {
        if (albumDetailsAdapter != null) {
            albumSongsList.clear();
            MainActivity.getSongFromMediastore(this);
            albumSongsList.addAll(albumSongsMap.get(albumName));
            albumDetailsAdapter.notifyDataSetChanged();

            if (albumSongsList.isEmpty()) {
                recyclerView.setVisibility(View.GONE);  // Show Song List
                noSongsText.setVisibility(View.VISIBLE);  // Show No song message
            } else {
                noSongsText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
        }
    }

    private BroadcastReceiver mediaScannerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshSongList();
        }
    };


    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction("actionPrevious");
        filter.addAction("actionPlay");
        filter.addAction("actionPause");
        filter.addAction("actionNext");

        filter.addDataScheme("file");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(mediaScannerReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mediaScannerReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(mediaContentObserver);
    }

}