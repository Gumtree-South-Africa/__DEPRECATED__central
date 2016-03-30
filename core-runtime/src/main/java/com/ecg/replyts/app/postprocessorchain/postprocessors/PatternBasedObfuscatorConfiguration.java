package com.ecg.replyts.app.postprocessorchain.postprocessors;

import com.ecg.replyts.core.runtime.EnvironmentSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Expects regular expressions as input in property {@code "postprocessors.obfuscater.patterns[0..n]"}. e.g.
 * <pre>
 *     postprocessors.obfuscater.patterns[0]=foo
 *     postprocessors.obfuscater.patterns[1]=bar
 * </pre>
 * creates a {@link PatternBasedObfuscater} with all those patterns (compiled with {@link Pattern#CASE_INSENSITIVE}
 * flag)
 *
 * @author alindhorst
 */
@Component
class PatternBasedObfuscatorConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(PatternBasedObfuscatorConfiguration.class);

    private List<Pattern> patterns;

    @Autowired
    public PatternBasedObfuscatorConfiguration(AbstractEnvironment environment) {
        patterns = EnvironmentSupport.propertyNames(environment)
          .stream()
          .filter(key -> key.startsWith("postprocessors.obfuscator.patterns["))
          .map(key -> {
              LOG.info("Removing all matches of /{}/ in outgoing mails.", environment.getProperty(key));

              return Pattern.compile(environment.getProperty(key), Pattern.CASE_INSENSITIVE);
          })
          .collect(Collectors.toList());
    }

    @Bean
    public PatternBasedObfuscater getObfuscater() {
        PatternBasedObfuscater patternBasedObfuscater = new PatternBasedObfuscater();
        //don't pass in the internal list to avoid external modification
        for (Pattern pattern : patterns) {
            patternBasedObfuscater.addPattern(pattern);
        }
        return patternBasedObfuscater;
    }
}
