package utils;

import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;

public class HttpClient {
    public final CloseableHttpClient client;

    public HttpClient(String ip, int port) {
        HttpHost superProxy = new HttpHost(ip, port);
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(5000).build();
        client = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(new BasicHttpClientConnectionManager())
                .setProxy(superProxy)
                .build();
    }

    public HttpClient() {
        client = HttpClients.createDefault();
    }
}