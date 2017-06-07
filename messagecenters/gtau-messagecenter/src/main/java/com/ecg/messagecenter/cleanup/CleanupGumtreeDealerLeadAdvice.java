package com.ecg.messagecenter.cleanup;

import org.apache.commons.lang3.StringUtils;

public class CleanupGumtreeDealerLeadAdvice extends AbstractCleanupAdvice {
    private static final String DEALER_LEAD_OPENING = "You have received a lead from Gumtree Australia regarding a";

    private static final String ENQUIRY_LINE = "Enquiry:";

    private static final String [] LINES_TO_KEEP = { "Name:", "Email:", "Phone:", ENQUIRY_LINE};

    protected CleanupGumtreeDealerLeadAdvice(Text text) {
        super(text);
    }

    @Override
    public void processAdvice() {

        boolean isDealerLeadLineDetected = false;
        boolean isDealerLineEnquiryLineDetected = false;
        for (Text.Line line : text.lines) {

            if (!text.getAdvice().isLineCleaned(line.originalIndex) && !text.getAdvice().isLineQuoted(line.originalIndex)) {

                if (line.content.contains(DEALER_LEAD_OPENING)) {
                    isDealerLeadLineDetected = true;
                    markQuoted(line.originalIndex);
                }
                else if (isDealerLeadLineDetected) {
                    if (line.content.contains(ENQUIRY_LINE)) {
                        isDealerLineEnquiryLineDetected = true;
                    }
                    if (!isDealerLineEnquiryLineDetected && StringUtils.indexOfAny(line.content, LINES_TO_KEEP) == -1) {
                        markQuoted(line.originalIndex);
                    }
                }
            }
        }
    }
}