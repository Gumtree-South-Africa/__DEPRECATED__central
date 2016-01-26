package com.ecg.replyts.app.postprocessorchain.postprocessors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

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
    public PatternBasedObfuscatorConfiguration(@Qualifier("replyts-properties") Properties p) {
        patterns = new ArrayList<Pattern>();
        for (Map.Entry<Object, Object> keyValue : p.entrySet()) {
            String key = keyValue.getKey().toString();
            if (key.startsWith("postprocessors.obfuscator.patterns[")) {
                String regularExpression = keyValue.getValue().toString();
                LOG.info("Removing all matches of /{}/ in outgoing mails.", regularExpression);
                patterns.add(Pattern.compile(regularExpression, Pattern.CASE_INSENSITIVE));
            }
        }
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
