package com.ecg.replyts.core.runtime.persistence.mail;

import com.ecg.replyts.core.api.model.mail.MailCreationDate;
import com.ecg.replyts.core.api.persistence.MailRepository;
import org.joda.time.DateTime;

import java.util.stream.Stream;

/**
 * Contains methods specific for getting or storing mail data in Cassandra.
 */
public interface CassandraMailRepository extends MailRepository {

    /**
     * Gets the mail creation date
     * @param mailId the mail id
     * @return the mail creation date or null if does not exist
     */
    DateTime getMailCreationDate(String mailId);

    /**
     * Streams mail ids and creation dates by the provided year, month and day.
     * @param year the year
     * @param month the month
     * @param day the day
     * @return the stream with mail ids and creation dates
     */
    Stream<MailCreationDate> streamMailCreationDatesByDay(int year, int month, int day);

    /**
     * Deletes the mail with the given id and creation date.
     * @param mailId the mail id
     * @param creationDate the creation date
     */
    void deleteMail(String mailId, DateTime creationDate);
}
