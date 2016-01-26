package com.ecg.replyts.core.runtime.cluster;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JvmIdentifierTest {

    @Test
    public void sameResultForCallingItTwice() {
        assertThat(new JvmIdentifier().getId()).isEqualTo(new JvmIdentifier().getId());
    }
}