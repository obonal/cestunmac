package com.cestunmac.android;

import java.util.HashSet;
import java.util.Iterator;

import android.support.v4.app.Fragment;
import android.util.Log;

public abstract class ISAMAbstractDataContainerFragment extends Fragment implements IDataRefresher {
    
    private HashSet<IDataRefreshListener> mRefreshListeners = new HashSet<IDataRefreshListener>();
    
    public void addDataRefreshListener(IDataRefreshListener l) {
        mRefreshListeners.add(l);
    }
    
    public void removeDataRefreshListener(IDataRefreshListener l) {
        mRefreshListeners.remove(l);
    }
    
    public void removeAllDataRefreshListener() {
        mRefreshListeners.clear();
        Log.d(Constants.LOG_TAG, " ******** " + getClass().getName() + " removeAllDataRefreshListener *********");
    }
    
    protected void fireDataRefreshBegin(String refresh_action_keyword) {
        Iterator<IDataRefreshListener> it = mRefreshListeners.iterator();
        while (it.hasNext()) {
            IDataRefreshListener l = (IDataRefreshListener) it.next();
            l.dataRefreshBegin(this, refresh_action_keyword);
        }
    }
    
    protected void fireDataRefreshEnd(String refresh_action_keyword) {
        Log.d(Constants.LOG_TAG, " ******** mRefreshListeners = " + mRefreshListeners + " *********");
        Iterator<IDataRefreshListener> it = mRefreshListeners.iterator();
        while (it.hasNext()) {
            IDataRefreshListener l = (IDataRefreshListener) it.next();
            l.dataRefreshEnd(this, refresh_action_keyword);
        }
    }
}
