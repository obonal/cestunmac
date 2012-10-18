package com.cestunmac.android.ui;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;

import com.cestunmac.android.R;
import com.cestunmac.android.data.Category;

public class CategoryCursorAdapter extends SimpleCursorAdapter {
    protected LayoutInflater   mInflater;
    protected Context          mContext;

    public CategoryCursorAdapter(Context context, Cursor c) {
        super(context, R.layout.category_cell, c, new String[] { Category.FIELD_TITLE }, new int[] { R.id.title }, 0);
        mContext = context;
        mInflater = LayoutInflater.from(context);
    }

//    @Override
//    public View getView(int position, View convertView, ViewGroup parent) {
//        View v;
//        if (convertView == null) {
//            v = newView(mContext, mCursor, parent);
//        } else {
//            v = convertView;
//        }
//        //View v = super.getView(position, convertView, parent);
//
//        Cursor c = (Cursor) getItem(position);
//        Category cat = Category.getCategoryFromCursor(c, mContext);
//
//        ImageView icone = (ImageView) v.findViewById(R.id.thumbnail);
//        icone.setImageDrawable(null);
////        if (imageDownloader.getDefault_img() == null) {
////            imageDownloader.setDefault_img(icone.getDrawable());
////        }
//        
//        
//        TextView title_view = (TextView) v.findViewById(R.id.title);
//        title_view.setText(cat.getTitle());
//
//        return v;
//    }
}