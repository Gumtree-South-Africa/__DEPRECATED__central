package com.ecg.replyts.core.webapi;

import com.ecg.replyts.core.runtime.cluster.XidFactory;
import com.ecg.replyts.core.runtime.logging.MDCConstants;
import com.ecg.replyts.core.webapi.CorrelationIdFilter;
import org.junit.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.PassThroughFilterChain;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import java.io.IOException;

import static com.ecg.replyts.core.webapi.CorrelationIdFilter.*;
import static org.assertj.core.api.Assertions.*;

public class CorrelationIdFilterTest {

    private CorrelationIdFilter victim = new CorrelationIdFilter();

    private MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();
    private MockHttpServletResponse mockHttpServletResponse = new MockHttpServletResponse();
    private MockFilterChain mockFilterChain = new MockFilterChain();

    @Test
    public void doFilter_shouldSetResponseHeader() throws IOException, ServletException {

        victim.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

        assertThat(mockHttpServletResponse.getHeader(CORRELATION_ID_HEADER)).isNotNull();
    }

    @Test
    public void doFilter_whenRequestHeader_shouldRepeatCorrelationId() throws IOException, ServletException {

        String correlationId = XidFactory.nextXid();

        mockHttpServletRequest.addHeader(CORRELATION_ID_HEADER, correlationId);

        victim.doFilter(mockHttpServletRequest, mockHttpServletResponse, mockFilterChain);

        assertThat(mockHttpServletResponse.getHeader(CORRELATION_ID_HEADER)).isEqualTo(correlationId);
    }

    @Test
    public void doFilter_shouldSetMDC() throws IOException, ServletException {

        Filter filter = new Filter() {

            @Override
            public void init(FilterConfig filterConfig) throws ServletException {
                throw new AssertionError();
            }

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
                assertThat(MDC.get(MDCConstants.CORRELATION_ID)).isNotNull();
            }

            @Override
            public void destroy() {
                throw new AssertionError();
            }
        };

        PassThroughFilterChain passThroughFilterChain = new PassThroughFilterChain(filter, mockFilterChain);

        victim.doFilter(mockHttpServletRequest, mockHttpServletResponse, passThroughFilterChain);
    }

    @Test
    public void doFilter_whenExceptionIsThrown_shouldClearMDC() throws IOException, ServletException {

        Filter filter = new Filter() {

            @Override
            public void init(FilterConfig filterConfig) throws ServletException {
                throw new AssertionError();
            }

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException {
                throw new ServletException("expected");
            }

            @Override
            public void destroy() {
                throw new AssertionError();
            }
        };

        PassThroughFilterChain passThroughFilterChain = new PassThroughFilterChain(filter, mockFilterChain);

        assertThatThrownBy(() -> victim.doFilter(mockHttpServletRequest, mockHttpServletResponse, passThroughFilterChain))
                .isInstanceOf(ServletException.class).hasMessage("expected");

        assertThat(MDC.get(MDCConstants.CORRELATION_ID)).isNull();
    }
}
