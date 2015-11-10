package nl.marktplaats.filter.volume;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import nl.marktplaats.filter.volume.VolumeFilterConfiguration.VolumeRule;
import nl.marktplaats.filter.volume.VolumeFilterFactory.VolumeFilterConfigParser;
import nl.marktplaats.filter.volume.persistence.VolumeFilterEventRepository;
import org.junit.Test;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VolumeFilterFactoryTest {

    VolumeFilterEventRepository repository = mock(VolumeFilterEventRepository.class);

    VolumeFilterConfigParser volumeFilterConfigParser = mock(VolumeFilterConfigParser.class);

    @Test
    public void shouldParseConfig() throws IOException {
        String json = "[" +
                "{" +
                "\"timeSpan\":10," +
                "\"timeUnit\":\"SECONDS\"," +
                "\"maxCount\":5," +
                "\"score\":20" +
                "}," +
                "{" +
                "\"timeSpan\":4," +
                "\"timeUnit\":\"MINUTES\"," +
                "\"maxCount\":6," +
                "\"score\":30" +
                "}" +
                "]";
        System.out.println(json);
        JsonNode configurationAsJson = new ObjectMapper().readTree(json);

        VolumeFilterConfigParser parser = new VolumeFilterConfigParser();
        VolumeFilterConfiguration configuration = parser.parse(configurationAsJson);
        assertThat(configuration.getConfig().size(), is(2));
        assertThat(configuration.getConfig().get(0).getTimeSpan(), is(10L));
        assertThat(configuration.getConfig().get(0).getTimeUnit(), is(TimeUnit.SECONDS));
        assertThat(configuration.getConfig().get(0).getMaxCount(), is(5L));
        assertThat(configuration.getConfig().get(0).getScore(), is(20));

        assertThat(configuration.getConfig().get(1).getTimeSpan(), is(4L));
        assertThat(configuration.getConfig().get(1).getTimeUnit(), is(TimeUnit.MINUTES));
        assertThat(configuration.getConfig().get(1).getMaxCount(), is(6L));
        assertThat(configuration.getConfig().get(1).getScore(), is(30));
    }

    @Test
    public void shouldCreateVolumeFilter() {
        List<VolumeRule> rules = Collections.singletonList(new VolumeRule(1L, TimeUnit.MINUTES, 10, 20));
        when(volumeFilterConfigParser.parse(new TextNode("bla"))).thenReturn(new VolumeFilterConfiguration(rules));
        VolumeFilterFactory volumeFilterFactory = new VolumeFilterFactory(repository, volumeFilterConfigParser);
        assertNotNull(volumeFilterFactory.createPlugin("", new TextNode("bla")));

    }

}