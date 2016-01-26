package com.ecg.replyts.core.runtime.persistence;

import com.basho.riak.client.RiakException;
import com.basho.riak.client.query.indexes.FetchIndex;
import org.joda.time.DateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FetchIndexHelperTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FetchIndex<Number> featchindex;

    @Test
    public void logInterval() throws Exception {
        // calculate interval
        assertThat(FetchIndexHelper.logInterval(10000)).isEqualTo(1000);
        // check min length
        assertThat(FetchIndexHelper.logInterval(1)).isEqualTo(100);
        assertThat(FetchIndexHelper.logInterval(1000000)).isEqualTo(10000);
    }

    @Test
    public void executeStreaming() throws RiakException {
        DateTime toTime = new DateTime();
        when(featchindex.from(anyInt())).thenReturn(featchindex);
        when(featchindex.to(any(Number.class))).thenReturn(featchindex);
        when(featchindex.maxResults(1000)).thenReturn(featchindex);

        FetchIndexHelper.fetchResult(featchindex, toTime, 1000);

        verify(featchindex).executeStreaming();
    }
}