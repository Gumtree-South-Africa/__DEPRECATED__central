package com.ecg.replyts.core.runtime.csv;

import java.io.Serializable;

/**
 * Should be implemented by any class which should be possible to convert from POJO to CSV
 */
public interface CsvSerializable extends Serializable {

    /**
     * Converts object (this) to a single CSV line in a form of string.
     * As <a href="https://tools.ietf.org/html/rfc4180">RFC 4180</a>, which covers CSV format, does not have strict requirements,
     * implementor can define any possible format as long as it meets common CSV criteria.
     *
     * @return - string representation of single CSV line
     */
    String toCsvLine();
}
