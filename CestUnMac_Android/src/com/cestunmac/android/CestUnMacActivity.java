package com.cestunmac.android;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.arellomobile.android.push.PushManager;
import com.cestunmac.android.ui.CategoriesTabFragment;
import com.cestunmac.android.ui.PostsTabFragment;

public class CestUnMacActivity extends SherlockFragmentActivity implements IDataRefreshListener,
    ILoaderEnabledActivity {

  public static final int TAB_INDEX_POSTS = 0;
  public static final int TAB_INDEX_CATEGORIES = 1;
  public static final int TAB_INDEX_WALLPAPERS = 2;
  private static final String APP_ID = "CBDFE-356EF";
  private static final String SENDER_ID = "481037532404";

  // private static final String STATE_IS_WORKING = "working";
  private ViewPager mPager;
  private TabsAdapter mTabsAdapter;

  // private boolean mIsWorking = false;

  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    PushManager pushManager = new PushManager(this, APP_ID, SENDER_ID);
    pushManager.onStartup(this);

    checkMessage(getIntent());

    requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
    setProgressBarIndeterminateVisibility(false);
    setContentView(R.layout.fragment_pager);
    getSupportActionBar().setDisplayHomeAsUpEnabled(false);

    mPager = (ViewPager) findViewById(R.id.pager);

    getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

    ActionBar.Tab tab1 = getSupportActionBar().newTab().setText(
        getText(R.string.posts_item_tabBarView));
    ActionBar.Tab tab2 = getSupportActionBar().newTab().setText(
        getText(R.string.categories_item_tabBarView));
    // ActionBar.Tab tab3 =
    // getSupportActionBar().newTab().setText(getText(R.string.wallpapers_item_tabBarView));

    mTabsAdapter = new TabsAdapter(this, getSupportActionBar(), mPager);
    mTabsAdapter.addTab(tab1, PostsTabFragment.class);
    mTabsAdapter.addTab(tab2, CategoriesTabFragment.class);
    // mTabsAdapter.addTab(tab3, WallPapersTabFragment.class);

    if (savedInstanceState != null) {
      getSupportActionBar().setSelectedNavigationItem(savedInstanceState.getInt("index"));
    }

  }

  @Override
  protected void onNewIntent(Intent intent)
  {
    super.onNewIntent(intent);
    setIntent(intent);

    checkMessage(intent);

    setIntent(new Intent());
  }

  private void checkMessage(Intent intent)
  {
    if (null != intent)
    {
      if (intent.hasExtra(PushManager.PUSH_RECEIVE_EVENT))
      {
        refreshCurrentFragment();
      }
//      else if (intent.hasExtra(PushManager.REGISTER_EVENT))
//      {
//      }
//      else if (intent.hasExtra(PushManager.UNREGISTER_EVENT))
//      {
//      }
//      else if (intent.hasExtra(PushManager.REGISTER_ERROR_EVENT))
//      {
//      }
//      else if (intent.hasExtra(PushManager.UNREGISTER_ERROR_EVENT))
//      {
//      }
    }
  }

  public void setWorkingStatus(final boolean working) {
    Log.d(Constants.LOG_TAG, " ===> setWorkingStatus(" + working + ")");

    // mIsWorking = working;
    runOnUiThread(new Runnable() {

      @Override
      public void run() {
        // getActionBarHelper().setRefreshActionItemState(working);
        setProgressBarIndeterminateVisibility(working);
        View prog = findViewById(R.id.empty_view_progress);
        // Log.w("DEBUG", " prog = " + prog);
        if (prog != null) {
          prog.setVisibility(working ? View.VISIBLE : View.GONE);
          TextView label = (TextView) findViewById(R.id.empty_view_label);
          label.setText(working ? R.string.loading_post_list_message
              : R.string.empty_post_list_message);
        }
      }
    });
  }

  // @Override
  // protected void onSaveInstanceState(Bundle outState) {
  // outState.putBoolean(STATE_IS_WORKING, mIsWorking);
  // super.onSaveInstanceState(outState);
  // }
  //
  // @Override
  // protected void onRestoreInstanceState(Bundle savedInstanceState) {
  // super.onRestoreInstanceState(savedInstanceState);
  // mIsWorking = savedInstanceState.getBoolean(STATE_IS_WORKING, false);
  // }
  //
  // @Override
  // protected void onResume() {
  // super.onResume();
  // setWorkingStatus(mIsWorking);
  // }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater menuInflater = getSupportMenuInflater();
    menuInflater.inflate(R.menu.main, menu);

    // Calling super after populating the menu is necessary here to ensure that
    // the
    // action bar helpers have a chance to handle this event.
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        // Toast.makeText(this, "Tapped home", Toast.LENGTH_SHORT).show();
        break;

      case R.id.menu_refresh:
        refreshCurrentFragment();
        break;
      case R.id.menu_wallpapers:
        if (mPager != null && mTabsAdapter != null) {
          // int current_item_index = mPager.getCurrentItem();
          // Fragment current_item = mTabsAdapter.getItem(current_item_index);

          Intent intent = new Intent();
          intent.setClass(this, WallpapersActivity.class);
          startActivity(intent);
        }
        break;

    // case R.id.menu_search:
    // Toast.makeText(this, "Tapped search", Toast.LENGTH_SHORT).show();
    // break;
    //
    // case R.id.menu_share:
    // Toast.makeText(this, "Tapped share", Toast.LENGTH_SHORT).show();
    // break;
    }
    return super.onOptionsItemSelected(item);
  }

  /**
     * 
     */
  public void refreshCurrentFragment() {
    if (mPager != null && mTabsAdapter != null) {
      int current_item_index = mPager.getCurrentItem();
      Fragment current_item = mTabsAdapter.getItem(current_item_index);
      if (current_item instanceof IDataRefresher) {
        // setWorkingStatus(true);
        IDataRefresher iDataRefresher = (IDataRefresher) current_item;
        iDataRefresher.addDataRefreshListener(CestUnMacActivity.this);
        iDataRefresher.refreshData(CestUnMacActivity.this);
      }
    }
  }

  /**
   * This is a helper class that implements the management of tabs and all
   * details of connecting a ViewPager with associated TabHost. It relies on a
   * trick. Normally a tab host has a simple API for supplying a View or Intent
   * that each tab will show. This is not sufficient for switching between
   * pages. So instead we make the content part of the tab host 0dp high (it is
   * not shown) and the TabsAdapter supplies its own dummy view to show as the
   * tab content. It listens to changes in tabs, and takes care of switch to the
   * correct paged in the ViewPager whenever the selected tab changes.
   */
  public static class TabsAdapter extends FragmentPagerAdapter implements
      ViewPager.OnPageChangeListener, ActionBar.TabListener {
    private final Context mContext;
    private final ActionBar mActionBar;
    private final ViewPager mViewPager;
    private final ArrayList<String> mTabs = new ArrayList<String>();

    public TabsAdapter(FragmentActivity activity, ActionBar actionBar, ViewPager pager) {
      super(activity.getSupportFragmentManager());
      mContext = activity;
      mActionBar = actionBar;
      mViewPager = pager;
      mViewPager.setAdapter(this);
      mViewPager.setOnPageChangeListener(this);
    }

    public void addTab(ActionBar.Tab tab, Class<?> clss) {
      mTabs.add(clss.getName());
      mActionBar.addTab(tab.setTabListener(this));
      notifyDataSetChanged();
    }

    @Override
    public int getCount() {
      return mTabs.size();
    }

    @Override
    public Fragment getItem(int position) {
      return Fragment.instantiate(mContext, mTabs.get(position), null);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageSelected(int position) {
      mActionBar.setSelectedNavigationItem(position);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
      mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }
  }

  @Override
  public void dataRefreshBegin(IDataRefresher refresher, String refresh_action_keyword) {
    if (Constants.POSTS_REFRESH_KEYWORD.equals(refresh_action_keyword) == false) {
      // Ignore other events
      return;
    }
    setWorkingStatus(true);
  }

  @Override
  public void dataRefreshEnd(IDataRefresher refresher, String refresh_action_keyword) {
    if (Constants.POSTS_REFRESH_KEYWORD.equals(refresh_action_keyword) == false) {
      // Ignore other events
      return;
    }
    setWorkingStatus(false);
    refresher.removeDataRefreshListener(this);

  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  public void morePostsClicked(View v) {
    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://cestunmac.com"));
    startActivity(browserIntent);
  }
}