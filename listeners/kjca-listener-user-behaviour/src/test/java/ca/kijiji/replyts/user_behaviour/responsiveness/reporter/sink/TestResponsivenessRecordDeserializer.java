package ca.kijiji.replyts.user_behaviour.responsiveness.reporter.sink;

import ca.kijiji.replyts.user_behaviour.responsiveness.model.ResponsivenessRecord;
import org.apache.kafka.common.serialization.Deserializer;

import java.time.Instant;
import java.util.Map;

public class TestResponsivenessRecordDeserializer implements Deserializer<ResponsivenessRecord> {

    @Override
    public void configure(Map<String, ?> map, boolean b) {
        // do nothing
    }

    @Override
    public ResponsivenessRecord deserialize(String s, byte[] bytes) {
        String csvLine = new String(bytes);
        String[] values = csvLine.split(",");
        if (values.length != 6) {
            throw new IllegalArgumentException("CSV line '" + csvLine + "' should contain 6 values");
        }
        return new ResponsivenessRecord(Integer.parseInt(values[0]), Long.parseLong(values[1]),
                values[2], values[3], Integer.parseInt(values[4]), Instant.ofEpochMilli(Long.parseLong(values[5])));
    }

    @Override
    public void close() {
        // do nothing
    }
}
