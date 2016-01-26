package com.ecg.replyts.core.webapi.util;


import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class EchoResponseGeneratorTest {

    @Test
    public void randomResponsesAreNotNull() {
        for (int i = 0; i < 100; i++) {
            String randomResponse = EchoResponseGenerator.randomEchoResponse();
            assertThat(randomResponse, is(notNullValue()));
        }
    }

    @Test
    public void defaultResponsesIsNotNull() {
        assertThat(EchoResponseGenerator.defaultEchoResponse(), is(notNullValue()));
    }

}
