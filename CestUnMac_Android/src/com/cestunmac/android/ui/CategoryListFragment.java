package com.cestunmac.android.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.cestunmac.android.CategoriesPostActivity;
import com.cestunmac.android.Constants;
import com.cestunmac.android.R;
import com.cestunmac.android.data.Category;

public class CategoryListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    boolean                       mDualPane;
    int                           mCurCheckPosition;
    // This is the Adapter being used to display the list's data.
    private CategoryCursorAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            // Restore last state for checked position.
            mCurCheckPosition = savedInstanceState.getInt("curChoice", 0);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(getString(R.string.empty_post_list_message));

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        // Populate list
        // loadData();

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
//        View detailsFrame = getActivity().findViewById(R.id.details);
//        mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;
        // No dual pane for categories
        mDualPane = false;
        
        initList(savedInstanceState);
    }

    /**
     * @param savedInstanceState
     */
    private void initList(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Restore last state for checked position.
            mCurCheckPosition = savedInstanceState.getInt("curChoice", 0);
        }

        loadData();

        Log.d(Constants.LOG_TAG, " ===> mCurCheckPosition = " + mCurCheckPosition + "; mDualPane = " + mDualPane);

        if (mDualPane) {
            // In dual-pane mode, the list view highlights the selected item.
            getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            // Make sure our UI is in the correct state.
            showDetails(mCurCheckPosition);
        } else {
            getActivity().setTitle(R.string.app_name);
        }

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initList(savedInstanceState);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("curChoice", mCurCheckPosition);
        Log.d(Constants.LOG_TAG, " ===> onSaveInstanceState SAVING: mCurCheckPosition = " + mCurCheckPosition);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        showDetails(position);
    }

    /**
     * Helper function to show the details of a selected item, either by
     * displaying a fragment in-place in the current UI, or starting a
     * whole new activity in which it is displayed.
     */
    private void showDetails(int index) {

        mCurCheckPosition = index;
        // Cursor c = cursorLoader.getCursor();
        Cursor c = mAdapter.getCursor();
        Log.i(Constants.LOG_TAG, "showDetails: c = " + c);
        long cat_id = -1;
        if (c != null) {
            c.moveToPosition(index);
            Category cat = new Category(c, getActivity());
            Log.i(Constants.LOG_TAG, "showDetails: category id: " + (cat == null ? "null" : "" + cat.getCategoryId()));
            cat_id = cat.getCategoryId();
        }

        Intent intent = new Intent();
        intent.setClass(getActivity(), CategoriesPostActivity.class);
        intent.putExtra("index", index);
        intent.putExtra(Constants.EXTRA_CATEGORY_ID, cat_id);
        startActivity(intent);

    }

    private void loadData() {
        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new CategoryCursorAdapter(getActivity(), null);
        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri content_uri = Constants.CATEGORIES_CONTENT_URI;
        String[] fieldProjection = null;
        String filter_criterion = null;
        String[] filter_args = null;
        String sort_criterion = Category.FIELD_TITLE;
        return new CursorLoader(getActivity(), content_uri, fieldProjection, filter_criterion, filter_args, sort_criterion + " COLLATE LOCALIZED ASC");
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }
}
