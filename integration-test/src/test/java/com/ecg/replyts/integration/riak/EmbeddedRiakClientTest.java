package com.ecg.replyts.integration.riak;

import com.basho.riak.client.IRiakObject;
import com.basho.riak.client.RiakException;
import com.basho.riak.client.RiakRetryFailedException;
import com.basho.riak.client.bucket.Bucket;
import com.basho.riak.client.builders.RiakObjectBuilder;
import com.basho.riak.client.cap.VClock;
import com.basho.riak.client.convert.ConversionException;
import com.basho.riak.client.convert.Converter;
import com.basho.riak.client.query.indexes.IntIndex;
import com.google.common.collect.Lists;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EmbeddedRiakClientTest {

    private EmbeddedRiakClient client = new EmbeddedRiakClient();

    @Test
    public void initializesClient() {
        new EmbeddedRiakClient();
    }

    @Test
    public void createsNewValue() throws RiakRetryFailedException {
        client.fetchBucket("foo").execute().store("var", "val").execute();
        assertEquals("val", client.fetchBucket("foo").execute().fetch("var").execute().getValueAsString());
    }

    @Test
    public void updatesValue() throws RiakRetryFailedException {
        client.fetchBucket("foo").execute().store("var", "val").execute();
        client.fetchBucket("foo").execute().store("var", "val2").execute();
        assertEquals("val2", client.fetchBucket("foo").execute().fetch("var").execute().getValueAsString());
    }

    @Test
    public void findIntIndexesByRange() throws RiakException {
        Bucket bucket = client.fetchBucket("foo").execute();

        bucket.store("var1", "val").withConverter(new IntIndexConverter(100)).execute();
        bucket.store("var2", "val").withConverter(new IntIndexConverter(200)).execute();
        bucket.store("var3", "val").withConverter(new IntIndexConverter(300)).execute();
        bucket.store("var4", "val").withConverter(new IntIndexConverter(400)).execute();

        assertEquals(Lists.newArrayList("var2", "var3"), bucket.fetchIndex(IntIndex.named("tmp")).from(120).to(330).execute());

    }

    @Test
    public void findIntIndexesExact() throws RiakException {
        Bucket bucket = client.fetchBucket("foo").execute();

        bucket.store("var1", "val").withConverter(new IntIndexConverter(100)).execute();
        bucket.store("var2", "val").withConverter(new IntIndexConverter(200)).execute();
        bucket.store("var3", "val").withConverter(new IntIndexConverter(300)).execute();

        assertEquals(Lists.newArrayList("var2"), bucket.fetchIndex(IntIndex.named("tmp")).withValue(200).execute());

    }

    private static class IntIndexConverter implements Converter<IRiakObject> {

        private final long indexVal;

        IntIndexConverter(long indexVal) {
            this.indexVal = indexVal;
        }


        @Override
        public IRiakObject fromDomain(IRiakObject domainObject, VClock vclock) throws ConversionException {
            return RiakObjectBuilder.newBuilder(domainObject.getBucket(), domainObject.getKey())
                    .withValue("val")
                    .withVClock(vclock)
                    .addIndex("tmp", indexVal)
                    .build();
        }

        @Override
        public IRiakObject toDomain(IRiakObject riakObject) throws ConversionException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
