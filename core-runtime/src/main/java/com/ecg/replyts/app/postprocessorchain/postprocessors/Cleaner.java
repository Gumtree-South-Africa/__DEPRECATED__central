package com.ecg.replyts.app.postprocessorchain.postprocessors;

import com.ecg.replyts.app.postprocessorchain.EmailPostProcessor;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Removes most email headers except those that are needed to maintain mail integrity.
 */
@Component
public class Cleaner implements EmailPostProcessor {
    public static final Set<String> HEADERS_TO_KEEP = new HashSet<String>(Arrays.asList(
            "Subject", "Date", "Content-Type", "Content-ID",
            "Content-Disposition", "Content-Transfer-Encoding", "MIME-Version",
            // The following headers are retained to prevent mail loops https://en.wikipedia.org/wiki/Email_loop
            "Precedence", "X-Precedence", "X-Auto-Response-Suppress", "Auto-Submitted"));

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void postProcess(MessageProcessingContext context) {
        MutableMail outgoingMail = context.getOutgoingMail();

        List<String> removeHeaders = new ArrayList<String>();
        for (Map.Entry<String, List<String>> entry : outgoingMail.getDecodedHeaders().entrySet()) {
            String headerName = entry.getKey();
            if (!HEADERS_TO_KEEP.contains(headerName)) {
                removeHeaders.add(headerName);
            }
        }

        for (String removeHeader : removeHeaders) {
            outgoingMail.removeHeader(removeHeader);
        }
    }
}
