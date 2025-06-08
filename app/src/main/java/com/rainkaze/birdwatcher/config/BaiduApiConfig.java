package com.rainkaze.birdwatcher.config;

/**
 * 百度API配置类，包含用于鉴权和访问百度AI平台的常量。
 */
public class BaiduApiConfig {

    // 百度API的应用API Key
    public static final String API_KEY = "ejCd6Fbyyn5SGWQx1LAlSwXr";

    // 百度API的应用Secret Key
    public static final String SECRET_KEY = "sDIKigGw9iNQnLMXkkq9XDzHUhk9odji";

    // 获取Access Token的接口地址
    public static final String TOKEN_ENDPOINT_URL = "https://aip.baidubce.com/oauth/2.0/token";

    // 授权类型：客户端凭证方式
    public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

    // 本地存储百度鉴权信息的文件名
    public static final String PREF_NAME_BAIDU_AUTH = "baidu_auth_prefs";

    // Access Token在本地存储中的键名
    public static final String KEY_ACCESS_TOKEN = "baidu_access_token";

    // Refresh Token在本地存储中的键名
    public static final String KEY_REFRESH_TOKEN = "baidu_refresh_token";

    // Access Token过期时间在本地存储中的键名
    public static final String KEY_EXPIRES_AT = "baidu_token_expires_at";

    // Access Token授权范围的键名
    public static final String KEY_SCOPE = "baidu_token_scope";
}
