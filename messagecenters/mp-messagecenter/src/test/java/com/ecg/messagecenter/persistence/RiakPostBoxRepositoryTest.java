package com.ecg.messagecenter.persistence;

import com.basho.riak.client.IRiakClient;
import com.ecg.replyts.integration.riak.EmbeddedRiakClient;

public class RiakPostBoxRepositoryTest extends AbstractPostBoxRepositoryTest<RiakPostBoxRepository> {

    private final IRiakClient riakClient = new EmbeddedRiakClient();

    @Override
    protected RiakPostBoxRepository createPostBoxRepository() {
        return new RiakPostBoxRepository(riakClient, null);
    }

}
