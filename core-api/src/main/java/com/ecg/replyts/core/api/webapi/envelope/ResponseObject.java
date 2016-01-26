package com.ecg.replyts.core.api.webapi.envelope;

/**
 * Base Class for all Webservice responses.
 *
 * @author huttar
 */
public final class ResponseObject<RESPONSE_TYPE> {

    private ProcessingStatus status = new ProcessingStatus(RequestState.OK, null);

    private RESPONSE_TYPE body;

    private PaginationInfo pagination;

    /**
     * Empty constructor for Jackson.
     */
    public ResponseObject() {
    }

    public static <R> ResponseObject<R> of(R response) {
        return new ResponseObject<R>(response);
    }

    public static ResponseObject<Void> of(RequestState state, String message) {
        ResponseObject<Void> res = new ResponseObject<Void>(null);
        res.setStatus(state, message);
        return res;
    }

    public static ResponseObject<Void> success(String message) {
        return of(RequestState.SUCCESS, message);
    }

    /**
     * @param body actual data returned
     */
    private ResponseObject(RESPONSE_TYPE body) {
        super();
        this.body = body;
    }

    /**
     * Sets the Responses actual data
     *
     * @param body the data that shall be transferred
     */
    public void setBody(RESPONSE_TYPE body) {
        this.body = body;
    }

    /**
     * @return the actual data of the document. May be null, when an error has occured on the server side
     */
    public RESPONSE_TYPE getBody() {
        return body;
    }

    /**
     * @return status of this response
     */
    public ProcessingStatus getStatus() {
        return status;
    }

    /**
     * Sets information about the number of results that were delivered
     *
     * @param from           first result retrieved
     * @param retrievedCount number of retrieved elements
     * @param totalCount     total number of elements
     */
    public void setPagination(int from, int retrievedCount, int totalCount) {
        this.pagination = new PaginationInfo(from, retrievedCount, totalCount);
    }

    /**
     * @return information about the number of results for a list-query
     */
    public PaginationInfo getPagination() {
        return pagination;
    }

    /**
     * @param state     State of this response
     * @param shortInfo short comparison of what had happened to this request
     */
    public void setStatus(RequestState state, String shortInfo) {
        this.status = new ProcessingStatus(state, shortInfo);
    }

}
