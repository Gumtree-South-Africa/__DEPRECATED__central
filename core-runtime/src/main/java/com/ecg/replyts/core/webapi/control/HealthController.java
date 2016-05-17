package com.ecg.replyts.core.webapi.control;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@RestController
@RequestMapping(value = "/health")
public class HealthController {
    @RequestMapping(method = RequestMethod.GET)
    public Health get() throws Exception {
        String version = getClass().getPackage().getImplementationVersion();

        return new Health(version);
    }

    class Health {
        String version;

        private Health(String version) {
            this.version = version;
        }

        public String getVersion() {
            return version;
        }
    }
}
