package io.jenkins.plugins.remote.result.trigger.utils.ssl;


import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * okhttp3忽略https证书
 *
 * @author HW
 */
public class SSLSocketManager {
    /**
     * 获取这个SSLSocketFactory
     *
     * @return SSLSocketFactory
     */
    @SuppressWarnings("lgtm[jenkins/unsafe-calls]")
    public static SSLSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, getTrustManager(), new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取trustManager
     *
     * @return TrustManager
     */
    @SuppressWarnings("lgtm[jenkins/unsafe-calls]")
    public static TrustManager[] getTrustManager() {
        return new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
        };
    }

    /**
     * 获取hostnameVerifier
     *
     * @return HostnameVerifier
     */
    @SuppressWarnings("lgtm[jenkins/unsafe-calls]")
    public static HostnameVerifier getHostnameVerifier() {
        return (s, sslSession) -> true;
    }
}
