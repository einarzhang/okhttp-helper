package com.einar.okhttp.helper.callback;

import okhttp3.Response;

public abstract class StringCallback extends ResponseCallback<String> {
    
    @Override
    public String responseToData(Response response) throws Exception {
        return response.body().string();
    }

}
