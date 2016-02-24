package nl.marktplaats.postprocessor.anonymizebody;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

public class AnonymizeEmailPostProcessorConfigTest {

    @Test
    public void testCreateConfig() throws Exception {
        Properties p = new Properties();
        p.setProperty("anonymizebody.pattern.0", "0");
        p.setProperty("anonymizebody.pattern.1000", "1000");
        p.setProperty("anonymizebody.pattern.200", "200");
        p.setProperty("anonymizebody.pattern.1", "1");
        p.setProperty("anonymizebody.pattern.50", "50");
        p.setProperty("anonymizebody.pattern.3", "3");

        AnonymizeEmailPostProcessorConfig config = new AnonymizeEmailPostProcessorConfig(p);

        List<String> patterns = config.getPatterns();
        Assert.assertEquals("0", patterns.get(0));
        Assert.assertEquals("1", patterns.get(1));
        Assert.assertEquals("3", patterns.get(2));
        Assert.assertEquals("50", patterns.get(3));
        Assert.assertEquals("200", patterns.get(4));
        Assert.assertEquals("1000", patterns.get(5));
    }
}