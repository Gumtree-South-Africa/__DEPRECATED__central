package com.ecg.replyts.app;

import com.ecg.replyts.core.runtime.mailparser.ParsingException;

import javax.annotation.WillNotClose;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public interface MessageProcessingCoordinator {
    /**
     * Invoked by a Mail Receiver. The passed input stream is an input stream the the actual mail contents. This method
     * will perform the full message processing and return once the message has reached an end state. If this method
     * throws an exception, an abnormal behaviour occured during processing, indicating the the Mail Receiver should try
     * to redeliver that message at a later time.
     * @return messageId of a processed message if the processing was completed
     */
    Optional<String> accept(@WillNotClose InputStream input) throws IOException, ParsingException;
}
