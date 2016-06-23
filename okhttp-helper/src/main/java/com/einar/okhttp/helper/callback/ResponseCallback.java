package com.einar.okhttp.helper.callback;

import okhttp3.Response;

/**
 * @author einarzhang
 */
public abstract class ResponseCallback<T> {

    public void processResponse(Response response) {
        if(response != null && response.isSuccessful()) {
            T data = null;
            try {
                data = responseToData(response);
            } catch (Exception e) {
                data = null;
            }
            doProcess(data);
        } else {
            doProcess(null);
        }
    }
    
    /**
     * 将response转为特定类型的数据
     * @param response
     * @return
     * @throws Exception
     */
    protected abstract T responseToData(Response response) throws Exception ;
    
    /**
     * 处理结果
     * @param data
     */
    protected abstract void doProcess(T data);
}
