package com.ecg.replyts.core.api.webapi.envelope;

/**
 * Response Status of a Webservice Request. If everything was fine, the status of any {@link ResponseObject} has a
 * {@link #state} of 200. In other cases, it may reassemble the HTTP Status codes
 *
 * @author huttar
 */
public class ProcessingStatus {

    private RequestState state;

    private String details;

    private String errorLog;

    // for jaxb binding
    public ProcessingStatus() {
    }

    public ProcessingStatus(RequestState state, String details) {
        super();
        this.state = state;
        this.details = details;
    }

    public ProcessingStatus(IllegalArgumentException ex) {
        this.state = RequestState.INVALID_ARGUMENTS;
        this.details = ex.getMessage();
        this.errorLog = buildStackTrace(ex);
    }


    private String buildStackTrace(Throwable t) {
        StringBuffer sb = new StringBuffer();
        for (StackTraceElement ste : t.getStackTrace()) {
            sb.append(ste.toString()).append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * @return In case of an Internal Server Error, this will actually contain the Errors Stack trace. Otherwise
     * <code>null</code>
     */
    public String getErrorLog() {
        return errorLog;
    }

    /**
     * @return the Processing State of this Request
     */
    public RequestState getState() {
        return state;
    }

    /**
     * @return a short description of the action that was performed or <code>null</code>
     */
    public String getDetails() {
        return details;
    }

    public void setState(RequestState state) {
        this.state = state;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void setErrorLog(String errorLog) {
        this.errorLog = errorLog;
    }
}
