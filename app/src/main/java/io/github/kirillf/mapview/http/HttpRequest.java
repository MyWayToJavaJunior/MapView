package io.github.kirillf.mapview.http;

import java.util.LinkedHashMap;
import java.util.Map;

public class HttpRequest {
    private final String url;
    private final Method method;
    private int readTimeout = 9000;
    private int connectionTimeout = 9000;
    private Map<String, String> headers;
    private String body;

    public HttpRequest(String url, Method method) {
        this.url = url;
        this.method = method;
        headers = new LinkedHashMap<>();
    }

    public String getUrl() {
        return url;
    }

    public Method getMethod() {
        return method;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public enum Method {
        GET("GET"),
        PUT("PUT"),
        POST("POST");

        private String requestParam;

        Method(String requestParam) {
            this.requestParam = requestParam;
        }

        public String getRequestParam() {
            return requestParam;
        }
    }
}
