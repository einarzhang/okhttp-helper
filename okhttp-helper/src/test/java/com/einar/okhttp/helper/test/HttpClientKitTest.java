package com.einar.okhttp.helper.test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import com.einar.okhttp.helper.HttpClientKit;

import okhttp3.FormBody;

public class HttpClientKitTest {

    @Test
    public void testBase() {
        Object data; 
        data = HttpClientKit.getOrCreate()
                .asyn(false)
                .get("http://fanyi.baidu.com/");
        System.out.println(data);
        Map<String, String> headers = new HashMap<String, String>() {
            {
//                put("Host", "fanyi.baidu.com");
//                put("Origin", "http://fanyi.baidu.com");
//                put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.79 Safari/537.36");
//                put("Referer", "http://fanyi.baidu.com/");
//                put("X-Requested-With", "XMLHttpRequest");
//                put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            }
        };
        FormBody body = new FormBody.Builder()
                .add("from", "zh")
                .add("to", "en")
                .add("query", "翻译")
                .add("transtype", "realtime")
                .add("simple_means_flag", "3")
                .build();
        data = HttpClientKit.getOrCreate(UUID.randomUUID().toString())
                .asyn(false)
                .setHeaders(headers)
                .post("http://fanyi.baidu.com/v2transapi", body);
        System.out.println(data);
    }

}
