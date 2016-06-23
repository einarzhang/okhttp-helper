package com.einar.okhttp.helper.requestbody;

import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * 主要用于创建Json请求内容等
 * @author einarzhang
 */
public class RequestBodyFactory {
	
	public static RequestBody createJson(String content) {
		return createJson(content, "UTF-8");
	}
	
    public static RequestBody createJson(String content, String charset) {
        return RequestBody.create(MediaType.parse("application/json;charset=" + charset), content);
    }
}
