package com.ecg.comaas.core.filter.ebayservices;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

public class HttpClientMockInterceptor implements Interceptor {

    private final OkHttpClient client = new OkHttpClient();

    @Override
    public Response intercept(Chain chain) throws IOException {
        String url = chain.request().url().toString();

        return MockStateHolder.containsKey(url)
                ? response(chain.request(), MockStateHolder.get(url))
                : client.newCall(chain.request()).execute();
    }

    private Response response(Request request, String body) {
        return new Response.Builder()
                .body(ResponseBody.create(MediaType.parse("application/json"), body))
                .code(200)
                .protocol(Protocol.HTTP_1_1)
                .request(request)
                .build();
    }
}
