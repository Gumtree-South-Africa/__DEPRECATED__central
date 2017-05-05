package com.ecg.replyts.core.webapi.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Manifest;

/**
 * COMAAS-355: gtuk temp version for debian package
 * <p>
 * This health endpoint is to provide gtuk with a debian package parsable version string, based on the packaging date.
 * Remove this class when we do the gtuk deploys in the cloud.
 */
@RestController
@RequestMapping(value = "/internal/properties/package/version")
public class GtukHealthController {
    private static final Logger LOG = LoggerFactory.getLogger(GtukHealthController.class);

    private static final String BUILD_DATE = "Build-Date";

    private String version = "-";

    @PostConstruct
    public void init() {
        try {
            Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if (!url.getFile().contains("/core-runtime-")) {
                    continue;
                }

                InputStream is = url.openStream();
                version = "5.0." + new Manifest(is).getMainAttributes().getValue(BUILD_DATE);
                is.close();
                break;
            }
        } catch (IOException ignore) {
        }
        LOG.debug("GTUK version has been set to {}", version);
    }

    @RequestMapping(method = RequestMethod.GET)
    public String get() {
        return String.format("{\"metadata\":{\"version\":\"%s\"}}", version);
    }
}
