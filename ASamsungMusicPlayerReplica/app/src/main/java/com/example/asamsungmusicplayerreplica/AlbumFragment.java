package com.example.asamsungmusicplayerreplica;

import static com.example.asamsungmusicplayerreplica.MainActivity.albumList;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;

public class AlbumFragment extends Fragment {

    // Adapter for recyclerView
    public static class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.AlbumViewHolder> {
        private Context albumContext;
        View albumItemView;

        public class AlbumViewHolder extends RecyclerView.ViewHolder{
            ImageView albumArt;
            TextView albumName;
            TextView artistName;

            public AlbumViewHolder(View itemView){
                super(itemView);
                albumArt = itemView.findViewById(R.id.album_item_album_art_card);
                albumName = itemView.findViewById(R.id.album_item_album_name_text);
                artistName = itemView.findViewById(R.id.album_item_artist_name_text);
            }
        }


        @Override
        public AlbumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            albumItemView = LayoutInflater.from(albumContext).inflate(R.layout.album_item, parent, false);
            return  new AlbumViewHolder(albumItemView);
        }

        public AlbumAdapter(Context albumContext) {
            this.albumContext = albumContext;
        }

        @Override
        public void onBindViewHolder(@NonNull AlbumViewHolder holder, @SuppressLint("RecyclerView") int position) {

            MainActivity.MusicFile musicFile = albumList.get(position);

            // Set song details
            holder.albumName.setText(musicFile.getAlbum());
            holder.albumName.setSelected(true);
            holder.artistName.setText(musicFile.getArtist());
            holder.artistName.setSelected(true);

            byte[] image = null;
            if (musicFile.getPath() != null) { // Check if the URI is not null
                image = getAlbumArt(musicFile.getPath());
            }
            if (image != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
                holder.albumArt.setImageBitmap(bitmap);  // Set the bitmap as the album art
            } else {
                holder.albumArt.setImageResource(R.drawable.default_ablum_art);  // Use placeholder image if no album art
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(albumContext, AlbumDetailsActivity.class);
                    intent.putExtra("albumName", albumList.get(position).getAlbum());
                    albumContext.startActivity(intent);
                }
            });

        }

        @Override
        public int getItemCount() {
            return albumList.size();
        }

        // get album art from uri
        // https://developer.android.com/reference/kotlin/android/media/MediaMetadataRetriever#getEmbeddedPicture()
        private byte[] getAlbumArt(String uri) {

            if (uri == null) {
                Log.e("AlbumAdapter", "getAlbumArt() - null URI");
                return null;
            }

            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                retriever.setDataSource(uri);
                return retriever.getEmbeddedPicture();
            } catch (IllegalArgumentException e) {
                Log.e("AlbumAdapter", "getAlbumArt() - Invalid URI: " + uri, e);
                return null;
            } catch (IllegalStateException e) {
                Log.e("AlbumAdapter", "getAlbumArt() -  invalid state.", e);
                return null;
            } finally {
                try {
                    retriever.release();
                } catch (RuntimeException e) {
                    Log.e("AlbumAdapter", "getAlbumArt() - Error on releasing MediaMetadataRetriever", e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    RecyclerView recyclerView;
    AlbumAdapter albumAdapter;

    public AlbumFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View albumListRecyclertView = inflater.inflate(R.layout.fragment_album, container, false);

        recyclerView = albumListRecyclertView.findViewById(R.id.albumfragment_album_recyclerview);
        TextView noSongsText = albumListRecyclertView.findViewById(R.id.albumfragment_album_list_no_songs_text);

        if (!albumList.isEmpty()) {
            albumAdapter = new AlbumAdapter(getContext());
            recyclerView.setAdapter(albumAdapter);
            recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

            noSongsText.setVisibility(View.GONE);  // Hide No song message
            recyclerView.setVisibility(View.VISIBLE);  // Show Song List

        } else {
            recyclerView.setVisibility(View.GONE);  // Show Song List
            noSongsText.setVisibility(View.VISIBLE);  // Show No song message
        }

        return albumListRecyclertView;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    // Helper Functions
    // refresh list when Mediastore content change (call in main - loadSong())

    public void refreshSongList() {
        if (albumAdapter != null) {

            albumList.clear();
            MainActivity.getSongFromMediastore(getContext());

            recyclerView = requireView().findViewById(R.id.albumfragment_album_recyclerview);
            TextView noSongsText = requireView().findViewById(R.id.albumfragment_album_list_no_songs_text);

            if (albumList.isEmpty()) {
                noSongsText.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                noSongsText.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);
            }
            albumAdapter.notifyDataSetChanged();
        }
    }
}