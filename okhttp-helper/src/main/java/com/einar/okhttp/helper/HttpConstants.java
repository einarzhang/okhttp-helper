package com.einar.okhttp.helper;

/**
 * http constants
 * @author einarzhang
 */
public class HttpConstants {
    
    public static final String DEFAULT_CLIENT_ID = "default_client_id";
    public static final long DEFAULT_CLIENT_MAX_IDLETIME= 20*60*1000L;
    
    /**
     * 返回数据类型定义
     */
    public static enum ReturnType { 
        TXT, BUFFERED_IMAGE
    }
    
}
