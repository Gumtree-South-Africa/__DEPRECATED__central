package com.ecg.replyts.gumtree.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import com.ecg.replyts.core.api.sanitychecks.Check;
import com.ecg.replyts.core.api.sanitychecks.CheckProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HealthCheckProvider {
    @Autowired(required = false)
    private List<CheckProvider> providers = new ArrayList();

    public List<NamedHealthCheck> getHealthChecks() {
        List<NamedHealthCheck> healthChecks = new ArrayList<>();
        for (CheckProvider provider : providers) {
            for (Check check : provider.getChecks()) {
                String name = String.format("%s - %s - %s", check.getCategory(), check.getSubCategory(), check.getName());
                healthChecks.add(new NamedHealthCheck(name, new CustomHealthCheck(check)));
            }
        }

        return healthChecks;
    }

    public static class NamedHealthCheck {
        private String name;
        private HealthCheck healthCheck;

        public NamedHealthCheck(String name, HealthCheck healthCheck) {
            this.name = name;
            this.healthCheck = healthCheck;
        }

        public String getName() {
            return name;
        }

        public HealthCheck getHealthCheck() {
            return healthCheck;
        }
    }
}