package com.ecg.messagecenter.it.chat;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.assertTrue;

/**
 * Created by jaludden on 22/12/15.
 */
public class TemplateTest {

    @Test public void testTemplateHasValidFormat() {

        Map<String, Object> variables = new HashMap<>();
        variables.put("from", "foo@bar.com");
        variables.put("message", "this is a message containing special characters, like the € currency, a quote like '" +
                ", a simple ampersand like & or a greater than like >, an esoteric accented vowel like é and " +
                "a simple backtick like ` .");
        variables.put("title", "foo");
        variables.put("ad_id", "123");
        variables.put("type", "annuncio");
        variables.put("greating", "foobar");
        variables.put("toSeller", true);
        variables.put("emailNickname", "foo_nick");

        String template = new Template("http://kijiji.it", "https://static.annuncicdn.it/it/images/")
                .createPostReplyMessage(variables);

        assertTrue(template.contains("quote like &#39;"));
        assertTrue(template.contains("the € currency"));
        assertTrue(template.contains("backtick like ` ."));
        assertTrue(template.contains("vowel like é"));
        assertTrue(template.contains("ampersand like &amp; "));
        assertTrue(template.contains("greater than like &gt;, "));
    }
}
