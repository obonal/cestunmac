package com.cestunmac.android.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import com.cestunmac.android.R;

public class WallPapersPagerAdapter extends PagerAdapter {

//    private final int[]    WALLPAPER_IDS = { R.drawable.wallpapers1, R.drawable.wallpapers2, R.drawable.wallpapers3, R.drawable.wallpapers4,
//            R.drawable.wallpapers5, R.drawable.wallpapers6, R.drawable.wallpapers7, R.drawable.wallpapers8, R.drawable.wallpapers9 };
    private final int[]    WALLPAPER_IDS = { R.drawable.wallpapers1_large, R.drawable.wallpapers2_large, R.drawable.wallpapers7_large, R.drawable.wallpapers9_large, R.drawable.wallpapers10_large, R.drawable.wallpapers11_large};
    private LayoutInflater mLayoutInflater;

    public WallPapersPagerAdapter(LayoutInflater inflater) {
        super();
        this.mLayoutInflater = inflater;
    }

    @Override
    public int getCount() {
        return WALLPAPER_IDS.length;
    }

    /**
     * Create the page for the given position. The adapter is responsible
     * for adding the view to the container given here, although it only
     * must ensure this is done by the time it returns from {@link #finishUpdate()}.
     * 
     * @param container
     *            The containing View in which the page will be shown.
     * @param position
     *            The page position to be instantiated.
     * @return Returns an Object representing the new page. This does not
     *         need to be a View, but can be some other container of the page.
     */
    @Override
    public Object instantiateItem(View collection, int position) {
        View v = mLayoutInflater.inflate(R.layout.image_display_view, null, false);
        ImageView iv = (ImageView) v.findViewById(R.id.img);
        if (WALLPAPER_IDS != null && WALLPAPER_IDS.length > 0 && WALLPAPER_IDS.length > position) {
            Bitmap bMap = BitmapFactory.decodeResource(iv.getResources(), WALLPAPER_IDS[position]);
            // TODO: Use dimens here?
            Bitmap resized = Bitmap.createScaledBitmap(bMap, bMap.getWidth() / 2, bMap.getHeight() / 2, true);
            iv.setImageBitmap(resized);
        }

        ((ViewPager) collection).addView(v, 0);
        return v;
    }

    /**
     * Remove a page for the given position. The adapter is responsible
     * for removing the view from its container, although it only must ensure
     * this is done by the time it returns from {@link #finishUpdate()}.
     * 
     * @param container
     *            The containing View from which the page will be removed.
     * @param position
     *            The page position to be removed.
     * @param object
     *            The same object that was returned by {@link #instantiateItem(View, int)}.
     */
    @Override
    public void destroyItem(View collection, int position, Object view) {
        ((ViewPager) collection).removeView((View) view);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((View) object);
    }

    /**
     * Called when the a change in the shown pages has been completed. At this
     * point you must ensure that all of the pages have actually been added or
     * removed from the container as appropriate.
     * 
     * @param container
     *            The containing View which is displaying this adapter's
     *            page views.
     */
    @Override
    public void finishUpdate(View arg0) {
    }

    @Override
    public void restoreState(Parcelable arg0, ClassLoader arg1) {
    }

    @Override
    public Parcelable saveState() {
        return null;
    }

    @Override
    public void startUpdate(View arg0) {
    }

    public int getImageResIdForPosition(int position) {
        if (position >= 0 && position < WALLPAPER_IDS.length) {
            return WALLPAPER_IDS[position];
        }
        return -1;
    }

}