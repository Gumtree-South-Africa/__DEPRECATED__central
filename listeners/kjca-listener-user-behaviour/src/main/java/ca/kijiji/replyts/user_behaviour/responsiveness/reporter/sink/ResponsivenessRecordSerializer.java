package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.sink;

import ca.kijiji.replyts.user_behaviour.responsiveness.model.ResponsivenessRecord;
import org.apache.kafka.common.serialization.Serializer;

import java.nio.charset.Charset;
import java.util.Map;

public class ResponsivenessRecordSerializer implements Serializer<ResponsivenessRecord> {

    @Override
    public void configure(Map<String, ?> map, boolean b) {
        // do nothing
    }

    @Override
    public byte[] serialize(String s, ResponsivenessRecord responsivenessRecord) {
        return responsivenessRecord.toCsvLine().getBytes(Charset.forName("UTF-8"));
    }

    @Override
    public void close() {
        // do nothing
    }
}
