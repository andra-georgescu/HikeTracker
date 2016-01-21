package com.andra.hiketracker.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.andra.hiketracker.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class PhotosAdapter extends RecyclerView.Adapter<PhotosAdapter.ViewHolder> {

    private final Context mContext;
    private List<String> mImageUris;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView mImageView;

        public ViewHolder(View v) {
            super(v);
            mImageView = (ImageView) v.findViewById(R.id.networkImage);
        }
    }

    public PhotosAdapter(Context context, List<String> imageUris) {
        mContext = context;
        mImageUris = imageUris;
    }

    @Override
    public PhotosAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.photo_view, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Picasso.with(mContext).load(mImageUris.get(position)).into(holder.mImageView);
    }

    @Override
    public int getItemCount() {
        return mImageUris.size();
    }
}
