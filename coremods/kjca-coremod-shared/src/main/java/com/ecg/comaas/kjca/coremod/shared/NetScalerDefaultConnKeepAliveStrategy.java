package com.ecg.comaas.kjca.coremod.shared;

import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpResponse;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

/**
 * Defines the keep-alive duration for a connection going through the NetScaler
 * and using its default keep-alive timeout of 180s. Respects the Keep-Alive
 * header if sent in the response.
 */
public class NetScalerDefaultConnKeepAliveStrategy implements ConnectionKeepAliveStrategy {

    private static final int NETSCALER_TIMEOUT_SEC = 170 * 1000; // It's actually 180 on the NS, but we want to account for
                                                                // any network delays and pad a bit just in case. It's better
                                                                // to close the connection on our side too early than wait for
                                                                // the NS to do it silently. The latter will cause an exception
                                                                // to be thrown from HttpClient.

    @Override
    public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
        // Honor 'keep-alive' header
        HeaderElementIterator it = new BasicHeaderElementIterator(
                response.headerIterator(HTTP.CONN_KEEP_ALIVE));
        while (it.hasNext()) {
            HeaderElement he = it.nextElement();
            String param = he.getName();
            String value = he.getValue();
            if (value != null && param.equalsIgnoreCase("timeout")) {
                try {
                    return Long.parseLong(value) * 1000;
                } catch(NumberFormatException ignore) {
                }
            }
        }

        return NETSCALER_TIMEOUT_SEC;
    }
}
