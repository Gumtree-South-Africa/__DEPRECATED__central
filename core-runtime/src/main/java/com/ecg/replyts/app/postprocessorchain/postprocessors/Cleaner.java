package com.ecg.replyts.app.postprocessorchain.postprocessors;

import com.ecg.replyts.app.postprocessorchain.PostProcessor;
import com.ecg.replyts.core.api.model.mail.MutableMail;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Removes most email headers except those that are needed to maintain mail integrity.
 */
@Component
public class Cleaner implements PostProcessor {
    public static final Set<String> HEADERS_TO_KEEP = new HashSet<String>(Arrays.asList(
            "Subject", "Date", "Content-Type", "Content-ID",
            "Content-Disposition", "Content-Transfer-Encoding", "MIME-Version"));

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
