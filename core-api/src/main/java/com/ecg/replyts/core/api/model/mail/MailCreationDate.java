package com.ecg.replyts.core.api.model.mail;

import com.ecg.replyts.core.api.util.Assert;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.Objects;

/**
 * Contains creation date for a mail.
 */
public class MailCreationDate {

    private final String mailId;
    private final Date creationDate;

    public MailCreationDate(String mailId, Date creationDate) {
        Assert.notNull(mailId);
        Assert.notNull(creationDate);
        this.mailId = mailId;
        this.creationDate = creationDate;
    }

    public MailCreationDate(String mailId, DateTime creationDateTime) {
        Assert.notNull(mailId);
        Assert.notNull(creationDateTime);
        this.mailId = mailId;
        this.creationDate = creationDateTime.toDate();
    }

    public String getMailId() {
        return mailId;
    }

    public DateTime getCreationDateTime() {
        return new DateTime(creationDate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MailCreationDate that = (MailCreationDate) o;
        return Objects.equals(mailId, that.mailId) &&
                Objects.equals(creationDate, that.creationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mailId, creationDate);
    }

    @Override
    public String toString() {
        return "MailCreationDate{" +
                "mailId='" + mailId + '\'' +
                ", creationDate=" + creationDate +
                '}';
    }
}
