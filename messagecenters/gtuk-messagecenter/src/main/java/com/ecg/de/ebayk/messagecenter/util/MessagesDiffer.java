package com.ecg.de.ebayk.messagecenter.util;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.runtime.TimingReports;
import com.gumtree.replyts2.common.message.MessageTextHandler;

/**
 * @author maldana@ebay-kleinanzeigen.de
 */
public class MessagesDiffer {

    private static final Timer CLEANUP_TIMER = TimingReports.newTimer("message-box.cleanup-timer");
    private static final Timer INIT_NGRAM = TimingReports.newTimer("message-box.init-ngram");
    private static final Timer FIND_MATCHES_TIMER = TimingReports.newTimer("message-box.find-matches-timer");
    private static final Timer APPLY_MATCHES_TIMER = TimingReports.newTimer("message-box.apply-matches-timer");

    public String cleanupFirstMessage(String firstMessage) {
        return MessageTextHandler.remove(firstMessage);
    }

    public TextDiffer.TextCleanerResult diff(DiffInput left, DiffInput right) {

        Timer.Context cleanupTimer = CLEANUP_TIMER.time();

        DiffInput cleanedLeft = new DiffInput(MessageTextHandler.remove(left.getText()), left.getConvId(), left.getMessageId());

        DiffInput cleanedRight = new DiffInput(MessageTextHandler.remove(right.getText()), right.getConvId(), right.getMessageId());

        long cleanupTime = cleanupTimer.stop();

        Timer.Context initNgram = INIT_NGRAM.time();
        TextDiffer leftTextDiffer = new TextDiffer(cleanedLeft);
        TextDiffer rightTextDiffer = new TextDiffer(cleanedRight);
        long initNgramsTime = initNgram.stop();

        Timer.Context findMatchesTimer = FIND_MATCHES_TIMER.time();

        for (TextDiffer.NGram nGram : leftTextDiffer.getNgramsOfText()) {
            rightTextDiffer.findMatch(nGram);
        }
        long findMatcherTime = findMatchesTimer.stop();

        Timer.Context applyMatchesTimer = APPLY_MATCHES_TIMER.time();
        TextDiffer.TextCleanerResult textCleanerResult = rightTextDiffer.cleanupMatches();
        long applyMatcherTime = applyMatchesTimer.stop();

        textCleanerResult.setProfilingInfo(
                new TextDiffer.TextCleanerResult.ProfilingInfo(
                        cleanupTime / (long) Math.pow(10, 6),
                        initNgramsTime / (long) Math.pow(10, 6),
                        findMatcherTime / (long) Math.pow(10, 6),
                        applyMatcherTime / (long) Math.pow(10, 6)
                )
        );

        return textCleanerResult;
    }

    public static class DiffInput {

        private String text;
        private String convId;
        private String messageId;

        DiffInput(String text, String convId, String messageId) {
            this.text = text;
            this.convId = convId;
            this.messageId = messageId;
        }

        public String getText() {
            return text;
        }

        public String getConvId() {
            return convId;
        }

        public String getMessageId() {
            return messageId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            DiffInput diffInput = (DiffInput) o;

            if (convId != null ? !convId.equals(diffInput.convId) : diffInput.convId != null) {
                return false;
            }
            if (messageId != null ? !messageId.equals(diffInput.messageId) : diffInput.messageId != null) {
                return false;
            }
            if (!text.equals(diffInput.text)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = text.hashCode();
            result = 31 * result + (convId != null ? convId.hashCode() : 0);
            result = 31 * result + (messageId != null ? messageId.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("DiffInput{");
            sb.append("size='").append(text.length()).append('\'');
            sb.append(", convId='").append(convId).append('\'');
            sb.append(", messageId='").append(messageId).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }
}
