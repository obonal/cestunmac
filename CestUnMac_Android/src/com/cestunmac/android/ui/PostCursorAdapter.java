package com.cestunmac.android.ui;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cestunmac.android.R;
import com.cestunmac.android.data.Category;
import com.cestunmac.android.data.Post;
import com.cestunmac.android.utils.ImageDownloader;

public class PostCursorAdapter extends SimpleCursorAdapter {
    protected LayoutInflater   mInflater;
    protected Context          mContext;

    public PostCursorAdapter(Context context, Cursor c) {
        super(context, R.layout.post_cell, c, new String[] { Post.FIELD_TITLE }, new int[] { R.id.title }, 0);
        mContext = context;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v;
        if (convertView == null) {
            v = newView(mContext, mCursor, parent);
        } else {
            v = convertView;
        }
        //View v = super.getView(position, convertView, parent);

        Cursor c = (Cursor) getItem(position);
        Post p = Post.getPostFromCursor(c, mContext);

        ImageView icone = (ImageView) v.findViewById(R.id.thumbnail);
        icone.setImageDrawable(null);
//        if (imageDownloader.getDefault_img() == null) {
//            imageDownloader.setDefault_img(icone.getDrawable());
//        }
        String thumbnailURLString = p.getThumbnailURLString();
        if (thumbnailURLString != null) {
            ImageDownloader.getInstance(mContext).download(thumbnailURLString, icone, ImageDownloader.FLAG_GET_THUMBNAIL);
        }
        
        TextView title_view = (TextView) v.findViewById(R.id.title);
        title_view.setText(p.getTitle());
        
        TextView subtitle_view = (TextView) v.findViewById(R.id.subtitle);
        Category category = p.getCategory();
        subtitle_view.setText(category!=null?category.getTitle():"");
        
        TextView date_view = (TextView) v.findViewById(R.id.date_field);
        Date date = new Date(p.getTimestamp());
        java.text.DateFormat dateFormat = new SimpleDateFormat("dd MMMM");
        date_view.setText(dateFormat.format(date));

        return v;
    }
}