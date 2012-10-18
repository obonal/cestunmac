package com.cestunmac.android;

import android.content.Context;

public interface IDataRefresher {
    public void refreshData(final Context _context);
    public void addDataRefreshListener(IDataRefreshListener l);
    public void removeDataRefreshListener(IDataRefreshListener l);
    public void removeAllDataRefreshListener();
}
