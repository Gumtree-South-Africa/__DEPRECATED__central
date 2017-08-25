package nl.marktplaats.filter.bankaccount;

import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.substring;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class BankAccountFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(BankAccountFilter.class);

    private static final int FILTER_REASON_DESCRIPTION_MAX_LENGTH = 255;
    private static final int FILTER_REASON_HINT_MAX_LENGTH = 255;
    private static final Pattern SIMPLE_WHITE_SPACE_PATTERN = Pattern.compile("[ \t]+");

    private BankAccountFinder bankAccountFinder;
    private DescriptionBuilder descriptionBuilder;

    public BankAccountFilter(BankAccountFinder bankAccountFinder, DescriptionBuilder descriptionBuilder) {
        this.bankAccountFinder = bankAccountFinder;
        this.descriptionBuilder = descriptionBuilder;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {
        Message message = context.getMessage();
        Conversation conv = context.getConversation();

        try {
            List<String> condensedTexts = extractMessageTexts(message).stream().map(text ->
                    SIMPLE_WHITE_SPACE_PATTERN.matcher(text).replaceAll(" ")
            ).collect(toList());

            List<FilterFeedback> result = new ArrayList<>();
            List<BankAccountMatch> bankAccountMatches = bankAccountFinder.findBankAccountNumberMatches(condensedTexts, conv.getAdId());
            for (BankAccountMatch banMatch : setAllScoresToZeroExceptFirstMaxScore(bankAccountMatches)) {
                List<Message> allMgs = conv.getMessages();
                Message firstMatchingMsg = allMgs.stream().filter(msg ->
                        !bankAccountFinder.containsSingleBankAccountNumber(banMatch.getBankAccount(), extractMessageTexts(msg), conv.getAdId()).isEmpty()
                ).findFirst().get();
                String description = descriptionBuilder.build(conv, banMatch, firstMatchingMsg, allMgs.size() - allMgs.indexOf(firstMatchingMsg));
                FilterFeedback feedback = new FilterFeedback(
                        substring(banMatch.getMatchedText(), 0, FILTER_REASON_HINT_MAX_LENGTH),
                        substring(description, 0, FILTER_REASON_DESCRIPTION_MAX_LENGTH),
                        banMatch.getScore(),
                        FilterResultState.OK);
                result.add(feedback);
            }
            return result;
        } catch (Exception e) {
            LOG.error("Was unable to detect bank account number in msg {}", message.getId(), e);
            return Collections.emptyList();
        } catch (Throwable e) {
            LOG.error("Could not process message with id {}", message.getId(), e);
            return Collections.emptyList();
        }
    }

    private List<BankAccountMatch> setAllScoresToZeroExceptFirstMaxScore(List<BankAccountMatch> originalMatches) {
        int highestScore = 0;
        int highestScoreIndex = 0;
        for (int i = 0; i < originalMatches.size(); i++) {
            BankAccountMatch match = originalMatches.get(i);
            if (match.getScore() > highestScore) {
                highestScore = match.getScore();
                highestScoreIndex = i;
            }
        }
        final int maxScoreIndex = highestScoreIndex;
        return IntStream.range(0, originalMatches.size()).mapToObj(i ->
                i == maxScoreIndex ? originalMatches.get(i) : originalMatches.get(i).withZeroScore()
        ).collect(toList());
    }

    private List<String> extractMessageTexts(Message message) {
        List<String> result = message.getTextParts().stream().filter(s -> !s.isEmpty()).collect(Collectors.toList());
        String subject = message.getHeaders().get("Subject");
        if (!isEmpty(subject)) result.add(subject);
        return result;
    }

}
