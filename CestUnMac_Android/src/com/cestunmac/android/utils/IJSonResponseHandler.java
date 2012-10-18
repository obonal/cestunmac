package com.cestunmac.android.utils;

import org.codehaus.jackson.JsonNode;

public interface IJSonResponseHandler {
    public void handleJSonResponse(JsonNode result_root_node);

    public void handleJSonRequestFailure(Throwable e);
}
