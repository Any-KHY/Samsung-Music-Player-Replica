package com.example.asamsungmusicplayerreplica;

import static com.example.asamsungmusicplayerreplica.MainActivity.songList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

public class SongsFragment extends Fragment {

    // Adapter for recyclerView
    public static class SongAdapter extends RecyclerView.Adapter<SongAdapter.MusicItemViewHolder>{

        private final Context songAdapterContext;

        public static class MusicItemViewHolder extends RecyclerView.ViewHolder{
            ImageView musicItemAlbumArt;
            TextView musicItemSongName;
            TextView musicItemArtistName;

            public MusicItemViewHolder(@NonNull View itemView) {
                super(itemView);
                musicItemAlbumArt = itemView.findViewById(R.id.music_item_album_art);
                musicItemSongName = itemView.findViewById(R.id.music_itme_song_name);
                musicItemArtistName = itemView.findViewById(R.id.music_itme_artist);
            }

        }

        @NonNull
        @Override
        public MusicItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new MusicItemViewHolder(LayoutInflater.from(songAdapterContext)
                    .inflate(R.layout.music_item, parent, false));
        }

        public SongAdapter(Context songAdapterContext) {
            this.songAdapterContext = songAdapterContext;
        }

        @Override
        public void onBindViewHolder(@NonNull MusicItemViewHolder holder, int position) {
            MainActivity.MusicFile musicFile = songList.get(position);

            // Set song details
            holder.musicItemSongName.setText(musicFile.getTitle());
            holder.musicItemSongName.setSelected(true);
            holder.musicItemArtistName.setText(musicFile.getArtist());
            holder.musicItemArtistName.setSelected(true);

            byte[] image = null;
            if (musicFile.getPath() != null) { // Check if the URI is not null
                image = getAlbumArt(musicFile.getPath());
            }

            if (image != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
                holder.musicItemAlbumArt.setImageBitmap(bitmap);
            } else {
                holder.musicItemAlbumArt.setImageResource(R.drawable.img_audiotrack);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(songAdapterContext, MusicPlayer.class);
                intent.putExtra("position", position);
                intent.putExtra("sender", "songAdapter");
                songAdapterContext.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            //return mainSongList.size();
            return songList.size();
        }

        // Helper Functions (Adaptor)

        // get album art from uri
        // https://developer.android.com/reference/kotlin/android/media/MediaMetadataRetriever#getEmbeddedPicture()
        private byte[] getAlbumArt(String uri) {

            if (uri == null) {
                Log.e("SongAdapter", "getAlbumArt() - null URI");
                return null;
            }

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(uri);
                return retriever.getEmbeddedPicture();
            } catch (IllegalArgumentException e) {
                Log.e("SongAdapter", "getAlbumArt() - Invalid URI: " + uri, e);
                return null;
            } catch (IllegalStateException e) {
                Log.e("SongAdapter", "getAlbumArt() -  invalid state.", e);
                return null;
            } finally {
                try {
                    retriever.release();
                } catch (RuntimeException e) {
                    Log.e("SongAdapter", "getAlbumArt() - Error on releasing MediaMetadataRetriever", e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

    }

    RecyclerView recyclerView;
    SongAdapter songAdapter;

    public SongsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View songListRecyclerView = inflater.inflate(R.layout.fragment_songs, container, false);

        recyclerView = songListRecyclerView.findViewById(R.id.songfragment_song_recyclerview);
        TextView noSongsText = songListRecyclerView.findViewById(R.id.songfragment_song_no_songs_text);

        if (!songList.isEmpty()) {
            songAdapter = new SongAdapter(getContext());
            recyclerView.setAdapter(songAdapter);
            recyclerView.setLayoutManager(
                    new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));

            noSongsText.setVisibility(View.GONE);  // Hide No song message
            recyclerView.setVisibility(View.VISIBLE);  // Show Song List

        } else {
            recyclerView.setVisibility(View.GONE);  // Show Song List
            noSongsText.setVisibility(View.VISIBLE);  // Show No song message
        }

        return songListRecyclerView;
    }

    // Helper Functions
    // refresh list when Mediastore content change (call in main - loadSong())
    public void refreshSongList() {
        if (songAdapter != null) {

            songList.clear();
            MainActivity.getSongFromMediastore(getContext());

            recyclerView = getView().findViewById(R.id.songfragment_song_recyclerview);
            TextView noSongsText = getView().findViewById(R.id.songfragment_song_no_songs_text);

            if (songList.isEmpty()) {
                noSongsText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                noSongsText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
            songAdapter.notifyDataSetChanged();
        }
    }
}