package com.ecg.replyts.core.api.sanitychecks;


/**
 * Interface for an Sanity Check.
 *
 * @author smoczarski
 */
public interface Check {

    /**
     * Perform the check an return the result of the check.
     * <p/>
     * Any exception thrown by the check will be converted to a CRITICAL result with a
     * message built from the exception
     *
     * @return The result of the check.
     * @throws Exception If an exception occurs during the check.
     */
    Result execute() throws Exception;

    /**
     * @return The name of the check.
     */
    String getName();

    /**
     * @return The category of this check.
     */
    String getCategory();

    /**
     * @return Optional sub category. If non, returns {@code null}.
     */
    String getSubCategory();
}
