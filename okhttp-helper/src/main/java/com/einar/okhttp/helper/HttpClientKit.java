package com.einar.okhttp.helper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.imageio.ImageIO;

import com.einar.okhttp.helper.HttpConstants.ReturnType;
import com.einar.okhttp.helper.callback.ResponseCallback;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpClientKit {

    private static Map<String, HttpClientKit> kitMap = new ConcurrentHashMap<String, HttpClientKit>();
    private static Lock lock = new ReentrantLock();
    private static boolean isCleanTaskInited = false;   //清理线程是否启动
    /** 当前HttpClientKit最后使用时间 */
    private long lastUseTimeMs;
    private ClientBuilder builder;
    private OkHttpClient httpClient;
    private Map<String, String> headers;

    private HttpClientKit(ClientBuilder builder) {
        this.lastUseTimeMs = System.currentTimeMillis();
        this.builder = builder;
        this.httpClient = builder.getBuilder().build();
        this.headers = new HashMap<String, String>();
    }

    public static final HttpClientKit getOrCreate() {
        return getOrCreate(HttpConstants.DEFAULT_CLIENT_ID);
    }
    
    public static final HttpClientKit getOrCreate(String clientId) {
        return getOrCreate(clientId, ClientBuilder.getDefault());
    }
    
    public static final HttpClientKit getOrCreate(ClientBuilder builder) {
        return getOrCreate(HttpConstants.DEFAULT_CLIENT_ID, builder);
    }

    public static final HttpClientKit getOrCreate(String clientId, ClientBuilder builder) {
        try {
            lock.lock();
            if(!isCleanTaskInited) {
                new Thread(new CleanTask()).start();
                isCleanTaskInited = true;
            }
            HttpClientKit kit = kitMap.get(clientId);
            // 需要判断当前Kit是否已失效
            if (kit == null || kit.needClean()) {
                if(builder == null) {
                    builder = ClientBuilder.getDefault();
                }
                kit = new HttpClientKit(builder);
                kitMap.put(clientId, kit);
            }
            return kit;
        } finally {
            lock.unlock();
        }
    }

    public Object get(String url) {
        return get(url, null, null);
    }

    public Object get(String url, final ResponseCallback<?> callback) {
        return get(url, null, callback);
    }

    public Object get(String url, Map<String, Object> params) {
        return get(url, params, null);
    }

    public Object get(String url, Map<String, Object> params, ResponseCallback<?> callback) {
        if (params != null && params.size() > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(url);
            sb.append("?");
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                sb.append(entry.getKey());
                sb.append("=");
                sb.append(entry.getValue());
                sb.append("&");
            }
            url = sb.toString().substring(0, sb.length() - 1);
        }
        okhttp3.Request.Builder reqBuilder = new Request.Builder();
        if(headers.size() > 0) {
            for(Entry<String, String> header : headers.entrySet()) {
                reqBuilder.addHeader(header.getKey(), header.getValue());
            }
            headers.clear();
        }
        Request req = reqBuilder.url(url).build();
        return reply(req, callback);
    }

    public Object post(String url) {
        return post(url, null, null);
    }
    
    public Object post(String url, ResponseCallback<?> callback) {
        return post(url, null, callback);
    }
    
    public Object post(String url, RequestBody body) {
        return post(url, body, null);
    }
    
    public Object post(String url, RequestBody body, ResponseCallback<?> callback) {
        okhttp3.Request.Builder reqBuilder = new Request.Builder();
        if(headers.size() > 0) {
            for(Entry<String, String> header : headers.entrySet()) {
                reqBuilder.addHeader(header.getKey(), header.getValue());
            }
            headers.clear();
        }
        Request req = reqBuilder.url(url).post(body).build();
        return reply(req, callback);
    }

    private Object reply(Request req, final ResponseCallback<?> callback) {
        Call call = httpClient.newCall(req);
        //不管成功失败，均更新lastUseTimeMs
        HttpClientKit.this.lastUseTimeMs = System.currentTimeMillis();
        if (builder.isAsyn) {
            call.enqueue(new Callback() {
                public void onResponse(Call call, Response response) throws IOException {
                    if (callback != null) {
                        callback.processResponse(response);
                    }
                }

                public void onFailure(Call call, IOException e) {
                    if (callback != null) {
                        callback.processResponse(null);
                    }
                }
            });
        } else {
            try {
                Response response = call.execute();
                if (response.isSuccessful()) {
                    if (HttpConstants.ReturnType.TXT.equals(builder.returnType)) {
                        return response.body().string();
                    } else if (HttpConstants.ReturnType.BUFFERED_IMAGE.equals(builder.returnType)) {
                        return ImageIO.read(response.body().byteStream());
                    }
                }
            } catch (IOException e) {
            }
        }
        return null;
    }
    
    /**
     * 同步异步设置快捷入口
     * @param isAsyn
     * @return
     */
    public HttpClientKit asyn(boolean isAsyn) {
        this.builder.isAsyn = isAsyn;
        return this;
    }

    /**
     * 返回类型设置快捷入口
     * @param returnType
     * @return
     */
    public HttpClientKit returnType(ReturnType returnType) {
        this.builder.returnType = returnType;
        return this;
    }
    
    public HttpClientKit addHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }
    
    public HttpClientKit addHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }
    
    public HttpClientKit setHeaders(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }
    
    /**
     * 是否需要清理
     * @param kit
     * @return
     */
    boolean needClean() {
       return  System.currentTimeMillis() - lastUseTimeMs > builder.maxIdleTimeMs;
    }
    
    /**
     * 清理线程 
     * @author einarzhang
     */
    public static class CleanTask implements Runnable {
        
        boolean flag = true;

        public void run() {
            while(flag) {
                Iterator<Entry<String, HttpClientKit>> it = kitMap.entrySet().iterator();
                while(it.hasNext()) {
                    Entry<String, HttpClientKit> entry = it.next();
                    if(entry.getValue().needClean()) {
                        it.remove();
                    }
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                }
            }
            isCleanTaskInited = false;
        }
        
    }
    

    /**
     * HttpClientKit构建工具，用于对HttpClientKit进行初始化前配置
     * 
     * @author einarzhang
     */
    public static final class ClientBuilder {
        /** kit最大空闲时间，超过最大空闲时间未使用，则该kit失效 */
        long maxIdleTimeMs;
        /** 标记请求是异步还是同步 =true表示异步 */
        boolean isAsyn;
        /** 返回的数据类型 */
        ReturnType returnType;
        /** OkHttp内建Builder */
        Builder builder;

        public ClientBuilder() {
        }

        public static ClientBuilder getDefault() {
            ClientBuilder builder = new ClientBuilder();
            builder.maxIdleTimeMs = HttpConstants.DEFAULT_CLIENT_MAX_IDLETIME;
            builder.isAsyn = true; // 默认为异步
            builder.returnType = ReturnType.TXT; // 默认为文本数据
            builder.builder = new Builder();
            return builder;
        }

        public HttpClientKit build() {
            return new HttpClientKit(this);
        }

        public ClientBuilder maxIdleTimeMs(long maxIdleTimeMs) {
            this.maxIdleTimeMs = maxIdleTimeMs;
            return this;
        }

        public ClientBuilder asyn(boolean isAsyn) {
            this.isAsyn = isAsyn;
            return this;
        }

        public ClientBuilder returnType(ReturnType returnType) {
            this.returnType = returnType;
            return this;
        }

        public ClientBuilder builder(Builder builder) {
            this.builder = builder;
            return this;
        }

        /**
         * OkHttp内建Builder，供需要高级功能使用，如代理等
         * 
         * @return
         */
        public Builder getBuilder() {
            return builder;
        }
    }
}
