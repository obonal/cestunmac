package com.cestunmac.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.cestunmac.android.ui.PostsTabFragment;

public class CategoriesPostActivity extends SherlockFragmentActivity implements IDataRefreshListener{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        if (savedInstanceState == null) {
            // During initial setup, plug in the details fragment.
            PostsTabFragment details = new PostsTabFragment();
            details.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(
                    android.R.id.content, details).commit();
        }
    }

    @Override
    public void dataRefreshBegin(IDataRefresher refresher, String refresh_action_keyword) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void dataRefreshEnd(IDataRefresher refresher, String refresh_action_keyword) {
        // TODO Auto-generated method stub
        
    }
    
    public void morePostsClicked(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://cestunmac.com"));
        startActivity(browserIntent);
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
    
}
