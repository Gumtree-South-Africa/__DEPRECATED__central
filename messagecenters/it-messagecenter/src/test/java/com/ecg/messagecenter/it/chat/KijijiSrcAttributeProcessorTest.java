package com.ecg.messagecenter.it.chat;

import org.junit.Before;
import org.junit.Test;
import org.thymeleaf.processor.attr.AbstractAttributeModifierAttrProcessor;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by jaludden on 22/12/15.
 */
public class KijijiSrcAttributeProcessorTest {

    private KijijiSrcAttributeProcessor p;
    private Map<String, Object> variables;

    @Before public void setUp() {
        p = new KijijiSrcAttributeProcessor("http://kijiji.it");
        variables = Collections.singletonMap("path", "test");
    }

    @Test public void testReplaceAttribute() {
        assertThat(p.getModificationType(null, null, null, null),
                        is(AbstractAttributeModifierAttrProcessor.ModificationType.SUBSTITUTION));
    }

    @Test public void testTargetAttributeName() {
        assertThat(p.getTargetAttributeName(null, null, null), is("src"));
    }

    @Test public void testGetSiteUrl() {
        assertThat(p.getHostUrl(), is("http://kijiji.it"));
    }

    @Test public void testReplaceWithoutExpression() {
        assertThat(p.replaceExpressions("path", Collections.emptyMap()), is("path"));
    }

    @Test public void testReplaceOnlyExpression() {
        assertThat(p.replaceExpressions("${path}", variables), is("test"));
    }

    @Test public void testReplaceExpressionStaticPartBefore() {
        assertThat(p.replaceExpressions("we_${path}", variables), is("we_test"));
    }

    @Test public void testReplaceExpressionStaticPartAfter() {
        assertThat(p.replaceExpressions("${path}_runs", variables), is("test_runs"));
    }

    @Test public void testReplaceExpressionStaticPartSournds() {
        assertThat(p.replaceExpressions("we_${path}_runs", variables), is("we_test_runs"));
    }
}
