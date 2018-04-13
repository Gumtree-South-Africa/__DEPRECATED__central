package com.ecg.comaas.bt.filter.word;

import com.ecg.replyts.core.api.util.JsonObjects;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PatternRulesParserTest {
    @Test(expected = IllegalArgumentException.class)
    public void rejectsJsonObjectWithoutRulesElement() throws Exception {
        PatternRulesParser.parse(JsonObjects.parse("{}"));
    }

    @Test(expected = IllegalArgumentException.class )
    public void rejectsBrokenRegularExpression() throws Exception {
        PatternRulesParser.parse(JsonObjects.parse("{rules: [{'regexp': '[', 'score': 20}]}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnparsableScroe() throws Exception {
        PatternRulesParser.parse(JsonObjects.parse("{rules: [{'regexp': '', 'score': 'as'}]}"));
    }

    @Test
    public void generatesCaseInsensitivePatterns() throws Exception {
        PatternEntry caseInsensPattern = PatternRulesParser.parse(JsonObjects.parse("{rules: [{'regexp': 'ab', 'score': 10}]}")).getPatterns().get(0);

        assertTrue(caseInsensPattern.getPattern().matcher("AB").find());
    }

    @Test
    public void extractsPatternEntries() throws Exception {
        List<PatternEntry> rules = PatternRulesParser.parse(JsonObjects.parse("{rules: [{'regexp': 'ab', 'score': 10}, {'regexp': 'bc', 'score': 20}]}")).getPatterns();

        assertEquals(2, rules.size());

        assertEquals("ab", rules.get(0).getPattern().toString());
        assertEquals(10, rules.get(0).getScore());

        assertEquals("bc", rules.get(1).getPattern().toString());
        assertEquals(20, rules.get(1).getScore());
    }

    @Test
    public void extractsPatternEntriesWithCategories() throws Exception {
        List<PatternEntry> rules = PatternRulesParser.parse(JsonObjects.parse("{rules: [{'regexp': 'ab', 'score': 10, 'categoryIds' : ['12', '216', 'cars']}]}")).getPatterns();

        assertEquals(1, rules.size());

        assertTrue(rules.get(0).getCategoryIds().isPresent());
        assertArrayEquals(rules.get(0).getCategoryIds().get().toArray(new String[0]), new String[] { "12", "216", "cars" });
    }
}
