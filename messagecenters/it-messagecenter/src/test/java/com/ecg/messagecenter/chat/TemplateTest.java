package com.ecg.messagecenter.chat;

import org.junit.Test;

import java.util.Collections;

/**
 * Created by jaludden on 22/12/15.
 */
public class TemplateTest {

    @Test public void testTemplatehasValidFormat() {
        new Template("http://kijiji.it", "https://static.annuncicdn.it/it/images/")
                        .createPostReplyMessage(Collections.emptyMap());
    }
}
