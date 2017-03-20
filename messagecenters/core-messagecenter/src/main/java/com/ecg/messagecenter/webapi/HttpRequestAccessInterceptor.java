package com.ecg.messagecenter.webapi;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HttpRequestAccessInterceptor extends HandlerInterceptorAdapter {
    private static ThreadLocal<HttpServletRequest> requestThreadLocal = new ThreadLocal<HttpServletRequest>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        requestThreadLocal.set(request);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // using afterCompletion() to also cover Exceptions
        requestThreadLocal.remove();
    }

    public static HttpServletRequest getHttpRequest() {
        return requestThreadLocal.get();
    }
}
