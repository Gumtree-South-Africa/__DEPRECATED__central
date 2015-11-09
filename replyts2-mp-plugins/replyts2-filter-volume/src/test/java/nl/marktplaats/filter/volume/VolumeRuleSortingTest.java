package nl.marktplaats.filter.volume;

import nl.marktplaats.filter.volume.VolumeFilterConfiguration.VolumeRule;
import nl.marktplaats.filter.volume.measure.RuleSorter;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.assertEquals;

public class VolumeRuleSortingTest {

    @Test
    public void testRuleSorting() {
        VolumeFilterConfiguration c = new VolumeFilterConfiguration();
        List<VolumeRule> rules = Arrays.asList(new VolumeRule(100l,
                TimeUnit.MINUTES, 30l, 40), new VolumeRule(200l,
                TimeUnit.SECONDS, 30l, 80), new VolumeRule(100l,
                TimeUnit.MINUTES, 30l, 30));
        c.setConfig(rules);

        List<VolumeRule> sortedRules = new RuleSorter().orderRules(c);
        assertEquals(80, sortedRules.get(0).getScore().intValue());
        assertEquals(30, sortedRules.get(2).getScore().intValue());
    }
}
