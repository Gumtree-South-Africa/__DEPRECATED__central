package com.ecg.replyts.app.mailreceiver;


/**
 * Marker interface for a Mail Data Provider. It will read mails from an arbitary source and pass it to the
 * {@link com.ecg.replyts.app.MessageProcessingCoordinator#accept(java.io.InputStream)} which will persist it and inform the preprocessor of a new mail.
 * If the method throws an exception it is to assume that the mail could not be handled right now (database down?) and
 * should be retried in a sensible amount of time.
 *
 * @author mhuttar
 */
public interface MailDataProvider extends Runnable {

    void prepareLaunch();

}
