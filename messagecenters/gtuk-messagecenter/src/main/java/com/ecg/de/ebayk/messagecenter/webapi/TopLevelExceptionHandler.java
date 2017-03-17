package com.ecg.de.ebayk.messagecenter.webapi;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * @author maldana@ebay-kleinanzeigen.de
 */
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
        LOGGER.error("Web Request error", ex);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        final StringWriter stringWriter= new StringWriter(2048);
        final PrintWriter printWriter = new PrintWriter(stringWriter);
        ex.printStackTrace(printWriter);

        final JSONObject error = new JSONObject();
        error.put("details", writer.toString());

        error.put("message", ex.getMessage());

        final JSONArray array = new JSONArray();
        array.add(error);

        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("errors", array);
        writer.write(jsonObject.toString());
    }


}
