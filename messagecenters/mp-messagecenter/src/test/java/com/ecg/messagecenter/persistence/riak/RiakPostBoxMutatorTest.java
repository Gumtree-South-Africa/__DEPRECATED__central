package com.ecg.messagecenter.persistence.riak;

import com.ecg.messagecenter.persistence.PostBox;
import com.ecg.messagecenter.persistence.riak.RiakPostBoxMutator;
import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;


public class RiakPostBoxMutatorTest {

    @Test
    public void takeNewPostboxIfOriginalIsNull() {
        PostBox inputPostBox = mock(PostBox.class);
        RiakPostBoxMutator mutator = new RiakPostBoxMutator(inputPostBox, Arrays.<String>asList());

        PostBox mutated = mutator.apply(null);

        assertThat(mutated).isSameAs(inputPostBox);
    }

    // other test-cases covered via PostBoxOverviewControllerAcceptanceTest (more large grained and easier instead
    // of mocking around...).

}