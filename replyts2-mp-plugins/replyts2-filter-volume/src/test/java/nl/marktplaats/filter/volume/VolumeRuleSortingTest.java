package nl.marktplaats.filter.volume;

import nl.marktplaats.filter.volume.VolumeFilterConfiguration.VolumeRule;
import org.junit.Test;
import java.util.Arrays;
import java.util.List;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

public class VolumeRuleSortingTest {

    @Test
    public void testRuleSorting() {
        List<VolumeRule> rules = Arrays.asList(
                new VolumeRule(100L, MINUTES, 30L, 40),
                new VolumeRule(200L, SECONDS, 30L, 80),
                new VolumeRule(100L, MINUTES, 30L, 30));
        VolumeFilterConfiguration c = new VolumeFilterConfiguration(rules);

        List<VolumeRule> sortedRules = new RuleSorter().orderRules(c);
        assertEquals(80, sortedRules.get(0).getScore());
        assertEquals(40, sortedRules.get(1).getScore());
        assertEquals(30, sortedRules.get(2).getScore());
    }
}
