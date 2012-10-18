package com.cestunmac.android;


import java.io.IOException;

import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.cestunmac.android.ui.WallPapersPagerAdapter;

public class WallpapersActivity extends SherlockFragmentActivity {
    private ViewPager mPager;
    private WallPapersPagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wallpapers_activity);
        findViewById(R.id.wallpapers_pager);
        mPager = (ViewPager) findViewById(R.id.wallpapers_pager);
        mPagerAdapter = new WallPapersPagerAdapter(getLayoutInflater());
        mPager.setAdapter(mPagerAdapter);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    public void setCurrentImageAsWallPaper(View v) {
        final int currentItem = mPager.getCurrentItem();
        final int currentItemResId = mPagerAdapter.getImageResIdForPosition(currentItem);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.set_as_wallpaper_confirmation_dialog_message)
               .setTitle(R.string.set_as_wallpaper_confirmation_dialog_title)
               .setCancelable(false)
               .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                       setWallPaperFromResId(currentItemResId);
                       WallpapersActivity.this.finish();
                   }
               })
               .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                   }
               });
        AlertDialog alert = builder.create();
        alert.show();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    protected void setWallPaperFromResId(int res_id) {
        if (res_id == -1) {
            return;
        }
        WallpaperManager wallpaperManager = WallpaperManager
                .getInstance(this);  
        try {
            wallpaperManager.setResource(res_id);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
