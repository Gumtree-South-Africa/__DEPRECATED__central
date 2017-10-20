package com.ecg.replyts.core.webapi;

import com.ecg.replyts.core.runtime.cluster.XidFactory;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import org.slf4j.MDC;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

public class CorrelationIdFilter implements Filter {

    static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // empty
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String correlationId = ((HttpServletRequest) request).getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null) {
           correlationId = XidFactory.nextXid();
        }

        try {
            ((HttpServletResponse) response).addHeader(CORRELATION_ID_HEADER, correlationId);

            MDC.put(MDCConstants.CORRELATION_ID, correlationId);

            chain.doFilter(request, response);
        }
        finally {
            MDC.clear();
        }
    }

    @Override
    public void destroy() {
        // empty
    }
}
