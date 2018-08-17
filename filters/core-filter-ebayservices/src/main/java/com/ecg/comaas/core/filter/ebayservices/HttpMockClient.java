package com.ecg.comaas.core.filter.ebayservices;

import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Locale;

public class HttpMockClient extends CloseableHttpClient {

    private final CloseableHttpClient httpClient;

    public HttpMockClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    protected CloseableHttpResponse doExecute(HttpHost httpHost, HttpRequest httpRequest, HttpContext httpContext) throws IOException {
        String url = httpRequest.getRequestLine().getUri();

        return MockStateHolder.containsKey(url)
                ? response(MockStateHolder.get(url))
                : httpClient.execute(httpHost, httpRequest, httpContext);
    }

    private CloseableHttpResponse response(String body) {
        return new HttpResponseProxy(body);
    }

    @Override
    public void close() throws IOException {
        httpClient.close();
    }

    @Override
    public HttpParams getParams() {
        return httpClient.getParams();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {
        return httpClient.getConnectionManager();
    }

    class HttpResponseProxy implements CloseableHttpResponse {

        private final StringEntity entity;

        HttpResponseProxy(String body) {
            entity = new StringEntity(body, Charset.defaultCharset());
        }

        public String toString() {
            return "HttpResponseProxy{" + this.entity + '}';
        }

        @Override
        public void close() throws IOException {
            // mock
        }

        @Override
        public StatusLine getStatusLine() {
            return new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), 200, "");
        }

        @Override
        public void setStatusLine(StatusLine statusLine) {
            //mock
        }

        @Override
        public void setStatusLine(ProtocolVersion protocolVersion, int i) {
            //mock
        }

        @Override
        public void setStatusLine(ProtocolVersion protocolVersion, int i, String s) {
            //mock
        }

        @Override
        public void setStatusCode(int i) throws IllegalStateException {
            //mock
        }

        @Override
        public void setReasonPhrase(String s) throws IllegalStateException {
            //mock
        }

        @Override
        public HttpEntity getEntity() {
            return entity;
        }

        @Override
        public void setEntity(HttpEntity httpEntity) {
            //mock
        }

        @Override
        public Locale getLocale() {
            return Locale.getDefault();
        }

        @Override
        public void setLocale(Locale locale) {
            //mock
        }

        @Override
        public ProtocolVersion getProtocolVersion() {
            return null;
        }

        @Override
        public boolean containsHeader(String s) {
            return false;
        }

        @Override
        public Header[] getHeaders(String s) {
            return new Header[0];
        }

        @Override
        public Header getFirstHeader(String header) {
            return "Content-Type".equals(header)
                    ? new BasicHeader("Content-Type", "text/xml")
                    : null;
        }

        @Override
        public Header getLastHeader(String s) {
            return null;
        }

        @Override
        public Header[] getAllHeaders() {
            return new Header[0];
        }

        @Override
        public void addHeader(Header header) {
            //mock
        }

        @Override
        public void addHeader(String s, String s1) {
            //mock
        }

        @Override
        public void setHeader(Header header) {
            //mock
        }

        @Override
        public void setHeader(String s, String s1) {
            //mock
        }

        @Override
        public void setHeaders(Header[] headers) {
            //mock
        }

        @Override
        public void removeHeader(Header header) {
            //mock
        }

        @Override
        public void removeHeaders(String s) {
            //mock
        }

        @Override
        public HeaderIterator headerIterator() {
            return null;
        }

        @Override
        public HeaderIterator headerIterator(String s) {
            return null;
        }

        @Override
        public HttpParams getParams() {
            return null;
        }

        @Override
        public void setParams(HttpParams httpParams) {
            //mock
        }
    }
}
