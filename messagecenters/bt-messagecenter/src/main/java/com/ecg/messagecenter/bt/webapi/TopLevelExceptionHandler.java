package com.ecg.messagecenter.bt.webapi;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class TopLevelExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TopLevelExceptionHandler.class);

    private Throwable exception;
    private HttpServletResponse response;
    private Writer writer;

    public TopLevelExceptionHandler(Throwable exception, HttpServletResponse response, Writer writer) {
        this.exception = exception;
        this.response = response;
        this.writer = writer;
    }

    public void handle() throws IOException {
        LOG.error("Top Level Exception: {}", exception.getMessage(), exception);

        setStatusAndWriteErrorMessage(exception, response, writer, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private void setStatusAndWriteErrorMessage(Throwable ex, HttpServletResponse response, Writer writer, int statusCode) throws IOException {
        response.setStatus(statusCode);

        writer.write(buildErrorWithMessage(ex).toString());
    }

    private JSONObject buildErrorWithMessage(Throwable ex) {
        StringWriter writer = new StringWriter(2048);

        ex.printStackTrace(new PrintWriter(writer));

        JSONObject error = new JSONObject();

        error.put("message", ex.getMessage());
        error.put("details", writer.toString());

        JSONArray array = new JSONArray();

        array.add(error);

        JSONObject jsonObject = new JSONObject();

        jsonObject.put("errors", array);

        return jsonObject;
    }
}