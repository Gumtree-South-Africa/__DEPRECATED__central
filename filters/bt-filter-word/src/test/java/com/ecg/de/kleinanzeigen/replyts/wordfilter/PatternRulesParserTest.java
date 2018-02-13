package com.ecg.de.kleinanzeigen.replyts.wordfilter;

import com.ecg.replyts.core.api.util.JsonObjects;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PatternRulesParserTest {

    @Test(expected = IllegalArgumentException.class)
    public void rejectsJsonObjectWithoutRulesElement() {
        new PatternRulesParser(JsonObjects.parse("{}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBrokenRegularExpression() {
        new PatternRulesParser(JsonObjects.parse("{rules: [{'regexp': '[', 'score': 20}]}"));

    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnparsableScroe() {
        new PatternRulesParser(JsonObjects.parse("{rules: [{'regexp': '', 'score': 'as'}]}"));
    }


    @Test
    public void generatesCaseInsensitivePatterns() {
        PatternEntry caseInsensPattern = new PatternRulesParser(JsonObjects.parse("{rules: [{'regexp': 'ab', 'score': 10}]}")).getConfig().getPatterns().get(0);

        assertTrue(caseInsensPattern.getPattern().matcher("AB").find());
    }

    @Test
    public void extractsPatternEntries() {
        List<PatternEntry> rules = new PatternRulesParser(JsonObjects.parse("{rules: [{'regexp': 'ab', 'score': 10}, {'regexp': 'bc', 'score': 20}]}")).getConfig().getPatterns();

        assertEquals(2, rules.size());

        assertEquals("ab", rules.get(0).getPattern().toString());
        assertEquals(10, rules.get(0).getScore());

        assertEquals("bc", rules.get(1).getPattern().toString());
        assertEquals(20, rules.get(1).getScore());
    }


    @Test
    public void extractsPatternEntriesWithCategories() {
        List<PatternEntry> rules = new PatternRulesParser(JsonObjects.parse("{rules: [{'regexp': 'ab', 'score': 10, 'categoryIds' : ['12', '216', 'cars']}]}")).getConfig().getPatterns();

        assertEquals(1, rules.size());
        assertTrue(rules.get(0).getCategoryIds().isPresent());
        assertThat((List<String>) rules.get(0).getCategoryIds().get(), hasItems("12", "216", "cars"));
    }
}
