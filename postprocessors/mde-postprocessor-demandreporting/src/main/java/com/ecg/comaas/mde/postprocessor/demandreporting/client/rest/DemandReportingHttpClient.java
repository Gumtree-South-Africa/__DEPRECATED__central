package com.ecg.comaas.mde.postprocessor.demandreporting.client.rest;

import java.io.IOException;

/**
 * An http client.
 * @author apeglow
 *
 */
public interface DemandReportingHttpClient {

	/**
	 * Posts json. Charset is utf8.
	 * @param url
	 * @param contentAsJson
	 * @throws IOException thrown when the request was not successful
	 */
	void postJson(String url, String contentAsJson) throws IOException;
	
	/**
	 * The number of concurrent connections this client allows.
	 * @return
	 */
	int getMaxConcurrentConnections();

}
