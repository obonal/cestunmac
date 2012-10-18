package com.cestunmac.android.ui;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.cestunmac.android.Constants;
import com.cestunmac.android.PostDetailsActivity;
import com.cestunmac.android.R;
import com.cestunmac.android.data.Category;
import com.cestunmac.android.data.Post;

public class PostListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    boolean           mDualPane;
    int               mCurCheckPosition;
    // This is the Adapter being used to display the list's data.
    PostCursorAdapter mAdapter;

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
        //setEmptyText(getString(R.string.empty_post_list_message));

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        // Populate list
        // loadData();

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        View detailsFrame = getActivity().findViewById(R.id.details);
        mDualPane = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

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

        Log.d(Constants.LOG_TAG, " ===> INITLIST: mCurCheckPosition = " + mCurCheckPosition + "; mDualPane = " + mDualPane);

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        int layout_id = R.layout.post_list;
        View v = inflater.inflate(layout_id, container, false);
        return v;
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
        // Log.d(Constants.LOG_TAG, " ===> onSaveInstanceState SAVING: mCurCheckPosition = " + mCurCheckPosition);
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
        // Log.i(Constants.LOG_TAG, "showDetails: c = " + c);
        int post_id = -1;
        if (c != null) {
            c.moveToPosition(index);
            Post p = new Post(c, getActivity());
            Log.i(Constants.LOG_TAG, "showDetails: post id: " + (p == null ? "null" : "" + p.getPostId()));
            post_id = p.getPostId();
        }
        if (mDualPane) {
            Log.i(Constants.LOG_TAG, "showDetails: dual pane...");
            // We can display everything in-place with fragments, so update
            // the list to highlight the selected item and show the data.
            getListView().setItemChecked(index, true);

            // Check what fragment is currently shown, replace if needed.
            PostDetailsFragment details = (PostDetailsFragment) getFragmentManager().findFragmentById(R.id.details);
            Log.i(Constants.LOG_TAG, "showDetails: details = " + details);
            if (details == null || details.getShownIndex() != index || details.getShownPostId() != post_id) {
                // Make new fragment to show this selection.
                details = PostDetailsFragment.newInstance(index, post_id);

                // Execute a transaction, replacing any existing fragment
                // with this one inside the frame.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.details, details);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }

        } else {
            // Otherwise we need to launch a new activity to display
            // the dialog fragment with selected text.
            Intent intent = new Intent();
            intent.setClass(getActivity(), PostDetailsActivity.class);
            intent.putExtra("index", index);
            intent.putExtra("postId", post_id);
            startActivity(intent);
        }
    }

    private void loadData() {
        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new PostCursorAdapter(getActivity(), null);
        setListAdapter(mAdapter);
        if (getListView().getFooterViewsCount() == 0) {
            View footerView = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.more_posts_footer, null,
                                                                                                                         false);
            getListView().addFooterView(footerView);
        }

        // Start out with a progress indicator.
        //setListShown(false);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri content_uri = Constants.POSTS_CONTENT_URI;
        String[] fieldProjection = null;
        String filter_criterion = null;
        String[] filter_args = null;

        if (getArguments() != null && getArguments().containsKey(Constants.EXTRA_CATEGORY_ID)) {
            long cat_id = getArguments().getLong(Constants.EXTRA_CATEGORY_ID, -1);
            if (cat_id != -1) {
                filter_criterion = Category.FIELD_CATEGORYID + "=?";
                filter_args = new String[] { "" + cat_id };
            }
        }
        String sort_criterion = Post.FIELD_DATE + " DESC";
        return new CursorLoader(getActivity(), content_uri, fieldProjection, filter_criterion, filter_args, sort_criterion);
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
        
        if (mDualPane && getView() != null && mAdapter != null) {
            Runnable r = new Runnable() {
                public void run() {
                    // In dual-pane mode, the list view highlights the selected item.
                    getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                    // Make sure our UI is in the correct state.
                    showDetails(mCurCheckPosition);
                }
            };
           new Handler().post(r);
        }

        // The list should now be shown.
//        if (isResumed()) {
//            setListShown(true);
//        } else {
//            setListShownNoAnimation(true);
//        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }
    
}
