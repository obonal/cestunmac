package com.cestunmac.android.ui;

import java.util.ArrayList;
import java.util.Iterator;

import org.codehaus.jackson.JsonNode;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cestunmac.android.Constants;
import com.cestunmac.android.IDataRefreshListener;
import com.cestunmac.android.ISAMAbstractDataContainerFragment;
import com.cestunmac.android.R;
import com.cestunmac.android.data.Post;
import com.cestunmac.android.utils.ServerExchangeUtils;

public class PostsTabFragment extends ISAMAbstractDataContainerFragment {

    public PostsTabFragment() {
        super();
        // Log.d(Constants.LOG_TAG, " ******** PostsTabFragment CONSTRUCTOR *********");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addDataRefreshListener((IDataRefreshListener) getActivity());
        refreshData(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // boolean is_landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        // int layout_id = is_landscape ? R.layout.fragment_layout_support_land : R.layout.fragment_layout_support;
        int layout_id = R.layout.fragment_layout_support;
        View v = inflater.inflate(layout_id, container, false);

        // PostListFragment titles = (PostListFragment)
        // getFragmentManager().findFragmentById(R.id.titles);
        // if (titles == null) {
        // Make new fragment to show this selection.
        PostListFragment titles = new PostListFragment();
        titles.setArguments(getArguments());

        // Execute a transaction, replacing any existing fragment
        // with this one inside the frame.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.titles, titles);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
        
        return v;
    }

    public void refreshData(final Context _context) {
        Runnable r = new Runnable() {
            public void run() {
                Looper.prepare();
                fireDataRefreshBegin(Constants.POSTS_REFRESH_KEYWORD);
                // loadData();
                try {
                    // TODO: Find out why getActivity() is null (or so it seems).
                    long timestamp = 0;
                    if (_context != null) {
                        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context);
                        timestamp = prefs.getLong(Constants.JSON_TIMESTAMP_KEYWORD, 0);
                    }
                    String request_url_string = (timestamp > 0 ? Constants.REQUEST_URL_POSTS + "&date=" + timestamp : Constants.REQUEST_URL_POSTS_FIRST_TIME);

                    JsonNode json_result = ServerExchangeUtils.getRequestForJSonResponse(request_url_string, _context);
                    if (json_result != null) {
                        handleJSonResponse(json_result, _context);
                    } else {
                        handleJSonRequestFailure();
                    }
                } catch (Throwable e) {
                    handleJSonRequestFailure();
                    e.printStackTrace();
                }
                // setWorkingStatus(false);
                Looper.loop();
            }

        };
        Thread t = new Thread(r, "Server Exchange Thread");
        t.start();
    }

    protected void handleJSonRequestFailure() {
        Log.e(Constants.LOG_TAG, "Request to server failed :-(");
        fireDataRefreshEnd(Constants.POSTS_REFRESH_KEYWORD);
    }

    protected void handleJSonResponse(JsonNode json_response, Context _context) {
        // Log.i(Constants.LOG_TAG, "Request Response: " + json_response);
        JsonNode status_node = ServerExchangeUtils.safeJSonGet(json_response, Constants.JSON_STATUSCODE_KEYWORD);
        String status = status_node == null ? null : status_node.getTextValue();
        if (Constants.OK_STATUS.equals(status)) {
            JsonNode timestamp_node = ServerExchangeUtils.safeJSonGet(json_response, Constants.JSON_TIMESTAMP_KEYWORD);
            long timestamp = timestamp_node == null ? 0 : timestamp_node.getLongValue();
            // Log.i(Constants.LOG_TAG, "timestamp: " + timestamp);
            JsonNode posts_array = ServerExchangeUtils.safeJSonGet(json_response, Constants.JSON_POSTS_KEYWORD);
            ArrayList<ContentValues> insert_values = new ArrayList<ContentValues>();
            Iterator<JsonNode> it = posts_array.getElements();
            while (it.hasNext()) {
                JsonNode post = (JsonNode) it.next();
                Post p = new Post(post, _context);
                if (p.isStoredInDb() == false) {
                    insert_values.add(p.getDBContentValues());
                }
            }

            if (insert_values.isEmpty() == false) {
                _context.getContentResolver().bulkInsert(Constants.POSTS_CONTENT_URI, insert_values.toArray(new ContentValues[] {}));
            }

            // Store timestamp
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context);
            Editor editor = prefs.edit();
            editor.putLong(Constants.JSON_TIMESTAMP_KEYWORD, timestamp);
            editor.commit();
        }
        Log.d(Constants.LOG_TAG, " ===> REFRESH DATA SUCCESSFULLY FINISHED!");
        fireDataRefreshEnd(Constants.POSTS_REFRESH_KEYWORD);
    }

}
