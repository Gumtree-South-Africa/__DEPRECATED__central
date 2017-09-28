package com.ecg.messagebox.controllers;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class TopLevelExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TopLevelExceptionHandler.class);

    private TopLevelExceptionHandler() {
        throw new AssertionError();
    }

    public static void handle(Throwable cause, HttpServletResponse response) throws IOException {
        LOGGER.error("Exception while processing web request: ", cause);

        // Only if not committed, reset is possible
        if (!response.isCommitted()) {
            // Need to reset, otherwise an error might be occurred using the writer if stream was used before on this response.
            response.reset();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

            try (PrintWriter writer = response.getWriter()) {
                JSONObject jsonObject = buildExceptionAsJson(cause);
                writer.write(jsonObject.toString());
            }
        }
    }

    private static JSONObject buildExceptionAsJson(Throwable cause) {
        StringWriter writer = new StringWriter(2048);
        cause.printStackTrace(new PrintWriter(writer));

        JSONObject jsonObject = new JSONObject();

        JSONArray array = new JSONArray();
        JSONObject error = new JSONObject();
        error.put("message", cause.getMessage());
        error.put("details", writer.toString());
        array.add(error);

        jsonObject.put("errors", array);
        return jsonObject;
    }
}