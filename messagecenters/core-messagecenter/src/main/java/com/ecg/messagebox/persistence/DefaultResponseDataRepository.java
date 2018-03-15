package com.ecg.messagebox.persistence;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Statement;
import com.ecg.messagebox.model.MessageType;
import com.ecg.messagebox.model.ResponseData;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.ecg.replyts.core.runtime.util.StreamUtils.toStream;

@Component
public class DefaultResponseDataRepository implements ResponseDataRepository {

    private final CassandraTemplate cassandraTemplate;
    private final int ttlResponseData;
    private final int limit;

    @Autowired
    public DefaultResponseDataRepository(CassandraTemplate cassandraTemplate,
                                         @Value("${persistence.cassandra.ttl.response.data:31536000}") int ttlResponseData,
                                         @Value("${responsedata.conv.limit:100}") int limit) {
        this.cassandraTemplate = cassandraTemplate;
        this.ttlResponseData = ttlResponseData;
        this.limit = limit;
    }

    @Override
    public List<ResponseData> getResponseData(String userId) {
        ResultSet result = cassandraTemplate.execute(Statements.SELECT_RESPONSE_DATA, userId, limit);
        return toStream(result)
                .map(this::rowToResponseData)
                .collect(Collectors.toList());
    }

    private ResponseData rowToResponseData(Row row) {
        return new ResponseData(row.getString("userid"), row.getString("convid"),
                new DateTime(row.getDate("createdate")), MessageType.getWithEmailAsDefault(row.getString("convtype")), row.getInt("responsespeed"));
    }

    @Override
    public void addOrUpdateResponseDataAsync(ResponseData responseData) {
        int secondsSinceConvCreation = Seconds.secondsBetween(responseData.getConversationCreationDate(), DateTime.now()).getSeconds();

        Statement bound = cassandraTemplate.bind(Statements.UPDATE_RESPONSE_DATA,
                ttlResponseData - secondsSinceConvCreation,
                responseData.getConversationType().name().toLowerCase(),
                responseData.getConversationCreationDate().toDate(),
                responseData.getResponseSpeed(),
                responseData.getUserId(),
                responseData.getConversationId());
        cassandraTemplate.executeAsync(bound);
    }
}
