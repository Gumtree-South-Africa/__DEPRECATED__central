package com.ecg.replyts.core.webapi.screeningv2;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;

/**
 * Does error handling, by displaying a standardized JSON response for all internal server errors.
 */
@Component
public final class ErrorHandlingController implements HandlerExceptionResolver {
    private static final Logger LOG = LoggerFactory.getLogger(ErrorHandlingController.class);

    @Override
    public ModelAndView resolveException(HttpServletRequest request,
                                         HttpServletResponse response, Object arg2, Exception ex) {
        LOG.error("Screening Webservice encountered Exception", ex);
        JsonNode n = JsonObjects.builder().failure(ex).build();
        try {
            String resp = JsonObjects.getObjectMapper().writeValueAsString(n);
            // Only if not committed, reset is possible
            if (!response.isCommitted()) {
                // Need to reset, otherwise an error might be occurred using the writer if stream was used before on this response.
                response.reset();
                response.setStatus(500);
                response.addHeader("Content-Type", "application/json;charset=utf-8");

                try (PrintWriter writer = response.getWriter()) {
                    writer.write(resp);
                }
            }
        } catch (Exception e) {
            LOG.error("Error handling error ;) ", e);
        }
        return new ModelAndView();
    }
}
