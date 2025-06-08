package com.rainkaze.birdwatcher.config;

public class BaiduApiConfig {

    // ！！！重要：请务必保护好你的 API Key 和 Secret Key ！！！
    // 在生产环境中，不建议直接硬编码在代码中。
    // 可以考虑使用 BuildConfig 字段从 local.properties 文件加载，或更安全的服务器端管理方式。
    public static final String API_KEY = "ejCd6Fbyyn5SGWQx1LAlSwXr";
    public static final String SECRET_KEY = "sDIKigGw9iNQnLMXkkq9XDzHUhk9odji";

    public static final String TOKEN_ENDPOINT_URL = "https://aip.baidubce.com/oauth/2.0/token";
    public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

    // SharedPreferences 文件名和键名
    public static final String PREF_NAME_BAIDU_AUTH = "baidu_auth_prefs";
    public static final String KEY_ACCESS_TOKEN = "baidu_access_token";
    public static final String KEY_REFRESH_TOKEN = "baidu_refresh_token"; // 虽然本次不直接用refresh_token流程，但仍保存
    public static final String KEY_EXPIRES_AT = "baidu_token_expires_at"; // 存储完整过期时间戳
    public static final String KEY_SCOPE = "baidu_token_scope";
}