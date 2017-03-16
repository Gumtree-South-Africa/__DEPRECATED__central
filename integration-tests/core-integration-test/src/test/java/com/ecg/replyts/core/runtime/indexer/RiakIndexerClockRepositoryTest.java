package com.ecg.replyts.core.runtime.indexer;

import com.ecg.replyts.integration.riak.EmbeddedRiakClient;
import org.junit.After;

public class RiakIndexerClockRepositoryTest extends AbstractIndexerClockRepositoryTest<RiakIndexerClockRepository> {

    @Override
    protected RiakIndexerClockRepository createClockRepository() throws Exception {
        return new RiakIndexerClockRepository(new EmbeddedRiakClient(), "");
    }

    @After
    public void tearDown() {
        getClockRepository().clear();
    }

}
