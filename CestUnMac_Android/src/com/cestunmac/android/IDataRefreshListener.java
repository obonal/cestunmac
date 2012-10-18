package com.cestunmac.android;

public interface IDataRefreshListener {
    public void dataRefreshBegin(IDataRefresher refresher, String refresh_action_keyword);
    public void dataRefreshEnd(IDataRefresher refresher, String refresh_action_keyword);
}
