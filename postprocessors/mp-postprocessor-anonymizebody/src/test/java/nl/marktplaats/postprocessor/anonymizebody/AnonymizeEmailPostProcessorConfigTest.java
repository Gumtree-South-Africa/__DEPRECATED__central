package nl.marktplaats.postprocessor.anonymizebody;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.List;
import java.util.Properties;

public class AnonymizeEmailPostProcessorConfigTest {

    @Test
    public void testCreateConfig() throws Exception {
        AbstractEnvironment environment = new StandardEnvironment();

        Properties p = new Properties();

        p.setProperty("message.normalization.pattern.0", "0");
        p.setProperty("message.normalization.pattern.1000", "1000");
        p.setProperty("message.normalization.pattern.200", "200");
        p.setProperty("message.normalization.pattern.1", "1");
        p.setProperty("message.normalization.pattern.50", "50");
        p.setProperty("message.normalization.pattern.3", "3");

        environment.getPropertySources().addFirst(new PropertiesPropertySource("test", p));

        AnonymizeEmailPostProcessorConfig config = new AnonymizeEmailPostProcessorConfig(environment);

        List<String> patterns = config.getPatterns();
        Assert.assertEquals("0", patterns.get(0));
        Assert.assertEquals("1", patterns.get(1));
        Assert.assertEquals("3", patterns.get(2));
        Assert.assertEquals("50", patterns.get(3));
        Assert.assertEquals("200", patterns.get(4));
        Assert.assertEquals("1000", patterns.get(5));
    }
}