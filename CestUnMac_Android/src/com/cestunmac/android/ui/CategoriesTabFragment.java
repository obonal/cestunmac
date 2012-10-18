package com.cestunmac.android.ui;

import java.util.Iterator;

import org.codehaus.jackson.JsonNode;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cestunmac.android.Constants;
import com.cestunmac.android.IDataRefreshListener;
import com.cestunmac.android.ISAMAbstractDataContainerFragment;
import com.cestunmac.android.R;
import com.cestunmac.android.data.Category;
import com.cestunmac.android.utils.ServerExchangeUtils;

public class CategoriesTabFragment extends ISAMAbstractDataContainerFragment {

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
        int layout_id = R.layout.categories_main;
        View v = inflater.inflate(layout_id, container, false);

        CategoryListFragment titles = new CategoryListFragment();

        // Execute a transaction, replacing any existing fragment
        // with this one inside the frame.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.category_list, titles);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();

        return v;
    }

    public void refreshData(final Context _context) {
        Runnable r = new Runnable() {
            public void run() {
                Looper.prepare();
                fireDataRefreshBegin(Constants.CATEGORIES_REFRESH_KEYWORD);
                try {
                    String request_url_string = Constants.REQUEST_URL_CATEGORIES;

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
        fireDataRefreshEnd(Constants.CATEGORIES_REFRESH_KEYWORD);
    }

    protected void handleJSonResponse(JsonNode json_response, Context _context) {
        Log.i(Constants.LOG_TAG, "Categories Request Response: " + json_response);
        JsonNode status_node = ServerExchangeUtils.safeJSonGet(json_response, Constants.JSON_STATUSCODE_KEYWORD);
        String status = status_node == null ? null : status_node.getTextValue();
        if (Constants.OK_STATUS.equals(status)) {
            JsonNode categories_array = ServerExchangeUtils.safeJSonGet(json_response, Constants.JSON_CATEGORIES_KEYWORD);
            if (categories_array != null) {
                Iterator<JsonNode> it = categories_array.getElements();
                while (it.hasNext()) {
                    JsonNode category_node = (JsonNode) it.next();
                    Category category = new Category(category_node, _context);
                    if ((category.getStoredDBId() == Constants.DB_STORAGE_ID_NOT_STORED)) {
                        category.persist(_context);
                    }
                }
            }
        }
        fireDataRefreshEnd(Constants.CATEGORIES_REFRESH_KEYWORD);
    }

}
