package com.ecg.replyts.core.api.webapi.envelope;

/**
 * Givs information about the pagination, if the result of an API call is a list of objects that contains more than one
 * element
 *
 * @author huttar
 */
public class PaginationInfo {

    private int from;

    private int deliveredCount;

    private int totalCount;

    public PaginationInfo(int from, int deliveredCount, int totalCount) {
        super();
        this.from = from;
        this.deliveredCount = deliveredCount;
        this.totalCount = totalCount;
    }

    /**
     * @return index of the first result element
     */
    public int getFrom() {
        return from;
    }

    /**
     * @return number of elements returned
     */
    public int getDeliveredCount() {
        return deliveredCount;
    }

    /**
     * @return total number of elements that match the given search.
     */
    public int getTotalCount() {
        return totalCount;
    }

}
