package com.ecg.messagecenter.webapi;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

class TopLevelExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopLevelExceptionHandler.class);

    private Throwable ex;
    private HttpServletResponse response;
    private Writer writer;

    public TopLevelExceptionHandler(Throwable ex, HttpServletResponse response, Writer writer) {
        this.ex = ex;
        this.response = response;
        this.writer = writer;
    }

    public void handle() throws IOException {
        LOGGER.error("Top Level Exception: " + ex.getMessage(), ex);
        setStatusAndWriteErrorMessage(ex, response, writer, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    private void setStatusAndWriteErrorMessage(Throwable ex, HttpServletResponse response, Writer writer, int statusCode) throws IOException {
        response.setStatus(statusCode);
        writer.write(buildErrorWithMessage(ex).toString());
    }

    private JSONObject buildErrorWithMessage(Throwable ex) {
        StringWriter writer = new StringWriter(2048);
        ex.printStackTrace(new PrintWriter(writer));

        JSONObject jsonObject = new JSONObject();

        JSONArray array = new JSONArray();
        JSONObject error = new JSONObject();
        error.put("message", ex.getMessage());
        error.put("details", writer.toString());
        array.add(error);

        jsonObject.put("errors", array);
        return jsonObject;
    }

}
