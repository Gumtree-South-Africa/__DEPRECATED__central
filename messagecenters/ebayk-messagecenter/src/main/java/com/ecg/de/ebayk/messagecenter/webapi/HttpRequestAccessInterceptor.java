package com.ecg.de.ebayk.messagecenter.webapi;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: maldana
 * Date: 12.02.14
 * Time: 17:28
 *
 * @author maldana@ebay.de
 */
class HttpRequestAccessInterceptor extends HandlerInterceptorAdapter {

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
