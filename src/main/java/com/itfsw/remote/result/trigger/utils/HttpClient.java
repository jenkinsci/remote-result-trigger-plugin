package com.itfsw.remote.result.trigger.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itfsw.remote.result.trigger.utils.ssl.SSLSocketManager;
import okhttp3.*;

import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.TimeUnit;


/**
 * Http Client
 */
public class HttpClient {
    private OkHttpClient client;
    private ObjectMapper mapper;

    /**
     * 构造函数
     *
     * @param client
     * @param mapper
     */
    public HttpClient(OkHttpClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    /**
     * 默认配置对象
     *
     * @return
     */
    public static HttpClient defaultInstance() {
        OkHttpClient client = new OkHttpClient()
                .newBuilder()
                .connectTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .build();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new HttpClient(client, mapper);
    }

    /**
     * 启用不安全的连接（不验证证书和域名）
     *
     * @return
     */
    public HttpClient useUnSafeSsl() {
        // 设置忽略证书和域名验证
        this.client = client.newBuilder()
                .sslSocketFactory(SSLSocketManager.getSSLSocketFactory(), (X509TrustManager) SSLSocketManager.getTrustManager()[0])
                .hostnameVerifier(SSLSocketManager.getHostnameVerifier())
                .build();
        return this;
    }

    /**
     * Request 请求
     *
     * @return
     */
    public Request request() {
        return new Request(this.client, this.mapper);
    }

    public static class Request extends okhttp3.Request.Builder {
        private OkHttpClient client;
        private ObjectMapper mapper;

        protected Request(OkHttpClient client, ObjectMapper mapper) {
            this.client = client;
            this.mapper = mapper;
        }

        /**
         * 发送POST请求(返回数据JSON转换)
         *
         * @param url       url 地址
         * @param params    参数
         * @param valueType 需要转换的类型
         * @return
         * @throws IOException
         */
        public <T> T doPost(String url, Map<String, Object> params, Class<T> valueType) throws IOException {
            // 1. 请求
            try (Response resp = doPost(url, params)) {
                if (resp.isSuccessful()) {
                    // 2. JSON 转换
                    String body = resp.body().string();
                    return mapper.readValue(body, valueType);
                }
            }
            return null;
        }

        /**
         * 发送GET请求(返回数据JSON转换)
         *
         * @param url       url 地址
         * @param params    参数
         * @param valueType 需要转换的类型
         * @return
         * @throws IOException
         */
        public <T> T doGet(String url, Map<String, Object> params, Class<T> valueType) throws IOException {
            // 1. 请求
            try (Response resp = doGet(url, params)) {
                if (resp.isSuccessful()) {
                    // 2. JSON 转换
                    String body = resp.body().string();
                    return mapper.readValue(body, valueType);
                }
            }
            return null;
        }

        /**
         * 发送POST请求
         *
         * @param url    url 地址
         * @param params 参数
         * @return
         * @throws IOException
         * @author hewei
         */
        public Response doPost(String url, Map<String, Object> params) throws IOException {
            // 2. 构建一个请求
            this.url(url);
            // 参数封装
            if (params != null) {
                FormBody.Builder formBuilder = new FormBody.Builder();
                for (String key : params.keySet()) {
                    formBuilder.add(key, params.get(key).toString());
                }
                this.post(formBuilder.build());
            }
            // 3. 创建一个Call
            final Call call = client.newCall(this.build());
            // 4. 执行请求
            Response resp = call.execute();
            return resp;
        }

        /**
         * 执行一个请求
         *
         * @param request 请求
         * @return
         * @throws IOException
         */
        public Response doRequest(okhttp3.Request request) throws IOException {
            return client.newCall(request).execute();
        }

        /**
         * 发送GET请求
         *
         * @param url    url 地址
         * @param params 参数
         * @return
         * @throws IOException
         * @author hewei
         */
        public Response doGet(String url, Map<String, Object> params) throws IOException {
            // 2. 构建一个请求
            String reqUrl;
            if (params == null) {
                reqUrl = url;
            } else {
                // 参数封装
                StringBuilder tempParams = new StringBuilder();
                int pos = 0;
                for (String key : params.keySet()) {
                    if (pos > 0) {
                        tempParams.append("&");
                    }
                    tempParams.append(String.format("%s=%s", key, URLEncoder.encode(params.get(key).toString(), "utf-8")));
                    pos++;
                }
                reqUrl = String.format("%s%s%s", url, url.matches(".*\\?.*") ? "" : "?", tempParams);
            }
            this.url(reqUrl);

            // 3. 创建一个Call
            final Call call = client.newCall(this.get().build());
            // 4. 执行请求
            Response resp = call.execute();
            return resp;
        }

        // --------------------------- overwrite ---------------------------

        /**
         * Sets the header named {@code name} to {@code value}. If this request already has any headers
         * with that name, they are all replaced.
         *
         * @param name
         * @param value
         */
        @Override
        public Request header(String name, String value) {
            return (Request) super.header(name, value);
        }

        /**
         * Adds a header with {@code name} and {@code value}. Prefer this method for multiply-valued
         * headers like "Cookie".
         *
         * <p>Note that for some headers including {@code Content-Length} and {@code Content-Encoding},
         * OkHttp may replace {@code value} with a header derived from the request body.
         *
         * @param name
         * @param value
         */
        @Override
        public Request addHeader(String name, String value) {
            return (Request) super.addHeader(name, value);
        }

        /**
         * Removes all headers named {@code name} on this builder.
         *
         * @param name
         */
        @Override
        public Request removeHeader(String name) {
            return (Request) super.removeHeader(name);
        }

        /**
         * Removes all headers on this builder and adds {@code headers}.
         *
         * @param headers
         */
        @Override
        public Request headers(Headers headers) {
            return (Request) super.headers(headers);
        }

        /**
         * Sets the URL target of this request.
         *
         * @param url
         * @throws IllegalArgumentException if the scheme of {@code url} is not {@code http} or {@code
         *                                  https}.
         */
        @Override
        public Request url(URL url) {
            return (Request) super.url(url);
        }

        /**
         * Sets the URL target of this request.
         *
         * @param url
         * @throws IllegalArgumentException if {@code url} is not a valid HTTP or HTTPS URL. Avoid this
         *                                  exception by calling {@link HttpUrl#parse}; it returns null for invalid URLs.
         */
        @Override
        public Request url(String url) {
            return (Request) super.url(url);
        }

        @Override
        public Request url(HttpUrl url) {
            return (Request) super.url(url);
        }
    }
}
