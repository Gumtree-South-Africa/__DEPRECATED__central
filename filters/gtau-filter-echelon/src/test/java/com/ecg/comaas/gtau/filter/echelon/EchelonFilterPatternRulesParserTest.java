package com.ecg.comaas.gtau.filter.echelon;

import com.ecg.replyts.core.api.util.JsonObjects;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EchelonFilterPatternRulesParserTest {

    @Test(expected = IllegalArgumentException.class)
    public void whenJsonEmpty_shouldThrowException() {
        EchelonFilterPatternRulesParser.fromJson(JsonObjects.parse("{}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenUrlNull_shouldThrowException() {
        EchelonFilterPatternRulesParser.fromJson(JsonObjects.parse("{'endpointUrl':null}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenUrlEmpty_shouldThrowException() {
        EchelonFilterPatternRulesParser.fromJson(JsonObjects.parse("{'endpointUrl':''}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenTimeoutNull_shouldThrowException() {
        EchelonFilterPatternRulesParser.fromJson(JsonObjects.parse("{endpointUrl:'foo.com',endpointTimeout:null}"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenTimeoutNotInt_shouldThrowException() {
        EchelonFilterPatternRulesParser.fromJson(JsonObjects.parse("{endpointUrl:'foo.com',endpointTimeout:'bla'}"));
    }

    @Test
    public void whenScoreNull_shouldReturnZeroScore() {
        EchelonFilterConfiguration configuration = EchelonFilterPatternRulesParser.fromJson(JsonObjects.parse("{endpointUrl:'foo.com',endpointTimeout:20,score:null}"));
        assertThat(configuration).isEqualToComparingFieldByField(
                new EchelonFilterConfiguration("foo.com", 20, 0));
    }

    @Test
    public void whenScoreNotInt_shouldReturnZeroScore() {
        EchelonFilterConfiguration configuration = EchelonFilterPatternRulesParser.fromJson(JsonObjects.parse("{endpointUrl:'foo.com',endpointTimeout:20,score:'bla'}"));
        assertThat(configuration).isEqualToComparingFieldByField(
                new EchelonFilterConfiguration("foo.com", 20, 0));
    }

    @Test
    public void whenAllFieldsCorrect_shouldReturnExpectedConfig() {
        EchelonFilterConfiguration configuration = EchelonFilterPatternRulesParser.fromJson(JsonObjects.parse("{endpointUrl:'foo.com',endpointTimeout:180,score:23}"));
        assertThat(configuration).isEqualToComparingFieldByField(
                new EchelonFilterConfiguration("foo.com", 180, 23));
    }
}
