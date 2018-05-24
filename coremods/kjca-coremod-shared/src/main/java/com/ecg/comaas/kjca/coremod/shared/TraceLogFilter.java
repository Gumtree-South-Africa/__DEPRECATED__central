package com.ecg.comaas.kjca.coremod.shared;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A servlet filter that automagically finds and binds a trace number into the TraceThreadLocal. Use of this class
 * handles the `set()` and `remove()` lifecycle calls.
 * <p>
 * When using this filter, consumers that do not pass the <code>X-Kijiji-TraceNumber</code> header will recieve a 412
 * response. This behaviour is intended to reduce the liklihood of non-compliant systems. In the rare case this filter
 * is an edge service and needs to generate its own traces, the init param, <code>generate</code>, may be used.
 */
public class TraceLogFilter implements Filter {

    public static final String TRACE_HEADER = "X-Kijiji-TraceNumber";

    private final AtomicBoolean generate = new AtomicBoolean(false);

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            throw new IllegalArgumentException("Unsupported request type.");
        }

        String trace = ((HttpServletRequest) request).getHeader(TRACE_HEADER);
        if (trace == null || trace.length() < 1) {
            if (!this.generate.get()) {
                // This request doesn't have a trace number. Reject it.
                ((HttpServletResponse) response).sendError(
                    HttpServletResponse.SC_PRECONDITION_FAILED,
                    "Request is missing X-Kijiji-TraceNumber.");
                return;
            } else {
                trace = UUID.randomUUID().toString();
            }
        }

        try {
            TraceThreadLocal.set(trace);
            chain.doFilter(request, response);
        } finally {
            TraceThreadLocal.reset();
        }
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        final String param = filterConfig.getInitParameter("generate");
        if (param != null) {
            this.generate.set(Boolean.parseBoolean(param));
        }
    }

    @Override
    public void destroy() {
    }
}
