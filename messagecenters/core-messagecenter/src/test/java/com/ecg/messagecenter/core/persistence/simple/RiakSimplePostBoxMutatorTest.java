package com.ecg.messagecenter.core.persistence.simple;

import com.ecg.messagecenter.core.persistence.simple.PostBox;
import com.ecg.messagecenter.core.persistence.simple.RiakSimplePostBoxMerger;
import com.ecg.messagecenter.core.persistence.simple.RiakSimplePostBoxMutator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class RiakSimplePostBoxMutatorTest {
    @Mock
    private PostBox postBox;

    @Test
    public void takeNewPostboxIfOriginalIsNull() {
        RiakSimplePostBoxMutator mutator = new RiakSimplePostBoxMutator(new RiakSimplePostBoxMerger(), postBox, Arrays.<String>asList());

        PostBox mutated = mutator.apply(null);

        assertThat(mutated).isSameAs(postBox);
    }

    // other test-cases covered via PostBoxOverviewControllerAcceptanceTest (more large grained and easier instead
    // of mocking around...).
}
