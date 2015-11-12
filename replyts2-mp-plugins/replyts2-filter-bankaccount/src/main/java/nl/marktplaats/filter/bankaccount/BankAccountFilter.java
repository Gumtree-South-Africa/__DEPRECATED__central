package nl.marktplaats.filter.bankaccount;

import com.ecg.replyts.app.Mails;
import com.ecg.replyts.core.api.model.MailCloakingService;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.FilterResultState;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.conversation.MessageDirection;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.MailAddress;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.api.pluginconfiguration.filter.Filter;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.ecg.replyts.core.runtime.mailparser.ParsingException;
import net.htmlparser.jericho.Source;
import nl.marktplaats.replyts2.util.MediaTypeHelper;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

import static org.apache.commons.lang.StringUtils.trimToEmpty;
import static org.apache.commons.lang.StringUtils.trimToNull;

public class BankAccountFilter implements Filter {

    private static final int FILTER_REASON_DESCRIPTION_MAX_LENGTH = 255;
    private static final int FILTER_REASON_HINT_MAX_LENGTH = 255;
    private static final Logger LOG = LoggerFactory.getLogger(BankAccountFilter.class);

    private static final Pattern SIMPLE_WHITE_SPACE_PATTERN = Pattern.compile("[ \t]+");

    private BankAccountFinder finder;
    private MailCloakingService mailCloakingService;
    private MailRepository mailRepository;
    private Mails mails;

    public BankAccountFilter(BankAccountFinder bankAccountFinder, MailCloakingService mailCloakingService, MailRepository mailRepository, Mails mails) {
        this.finder = bankAccountFinder;
        this.mailCloakingService = mailCloakingService;
        this.mailRepository = mailRepository;
        this.mails = mails;
    }

    private List<String> condenseWhitespaces(List<String> texts) {
        List<String> result = new ArrayList<String>(texts.size());
        for (String text : texts) {
            result.add(SIMPLE_WHITE_SPACE_PATTERN.matcher(text).replaceAll(" "));
        }
        return result;
    }

    private List<FilterFeedback> convertToMatches(
            List<BankAccountMatch> matches, Message message, Conversation conv, Mail mail) throws Exception {

        if (matches.isEmpty()) return Collections.emptyList();

        // To get all preceding messages in the conversation the conversation needs to be enriched.
        // However, when the current message is the first in a conversation we can skip this.
        // As 60% of all messages are actually the first in a conversation, doing the skip is an important
        // performance optimization.
        List<Message> allConversationMessages;
        List<Message> precedingMessages;
        if (isFirstMailInConversation(mail)) {
            allConversationMessages = Collections.singletonList(message);
            precedingMessages = Collections.emptyList();

        } else {
            allConversationMessages = conv.getMessages();
            precedingMessages = allConversationMessages.subList(0, allConversationMessages.size() - 1);
        }

        // Extract bank account numbers that matched
        Set<String> matchedBankAccountNumbers = new HashSet<String>(matches.size());
        for (BankAccountMatch match : matches) {
            matchedBankAccountNumbers.add(match.getBankAccount());
        }

        // Convert each ban to a MailAndMessageInConversation
        Map<String, MailAndMessageInConversation> matchesToFirstMail =
                new HashMap<String, MailAndMessageInConversation>(matches.size());
        for (String ban : matchedBankAccountNumbers) {
            matchesToFirstMail.put(ban, findFirstWithBan(ban, mail, message, conv, precedingMessages));
        }

        // Convert each match to a reason
        List<FilterFeedback> result = new ArrayList<>(matches.size());
        for (BankAccountMatch banMatch : matches) {
            MailAndMessageInConversation firstWithBan = matchesToFirstMail.get(banMatch.getBankAccount());

            // BAN = bank account number
            Mail firstMailWithBan = firstWithBan.mail;
            Message firstMessageWithBan = firstWithBan.message;
            int mailMatchCount = allConversationMessages.size() - firstWithBan.indexInConversation;

            String description = toDescription(conv, banMatch, firstMailWithBan, firstMessageWithBan, mailMatchCount);
            result.add(new FilterFeedback(
                    StringUtils.substring(banMatch.getMatchedText(), 0, FILTER_REASON_HINT_MAX_LENGTH),
                    StringUtils.substring(description, 0, FILTER_REASON_DESCRIPTION_MAX_LENGTH),
                    banMatch.getScore(),
                    FilterResultState.OK));
        }
        return result;
    }

    private List<String> extractMailTexts(Mail mail) {
        List<String> texts = new ArrayList<String>(3);

        String subject = mail.getSubject();
        if (subject != null) texts.add(subject);

        List<TypedContent<String>> textParts = mail.getTextParts(false);
        for (TypedContent<String> part : textParts) {
            if (MediaTypeHelper.isHtmlCompatibleType(part.getMediaType())) {
                // Render the HTML
                // long startRender = System.currentTimeMillis();
                Source source = new Source(part.getContent());
                source.setLogger(null);
                texts.add(source.getRenderer().toString());
                // System.out.println("Jericho render took " + (System.currentTimeMillis() - startRender));
            } else if (MediaTypeHelper.isPlainTextCompatible(part.getMediaType())) {
                // Plain text, copy as is.
                String plainTextContent = part.getContent();
                if (plainTextContent != null) texts.add(plainTextContent);

            }
            // else: skip unknown parts
        }
        return texts;
    }

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext context) {
        Mail mail = context.getMail();
        Message message = context.getMessage();
        Conversation conv = context.getConversation();

        try {
            List<String> texts = extractMailTexts(mail);
            List<String> condensedTexts = condenseWhitespaces(texts);
            List<BankAccountMatch> allMatches = finder.findBankAccountNumberMatches(condensedTexts, conv.getAdId());
            makeSureOnly1HighestMatchGivesAScore(allMatches);
            return convertToMatches(allMatches, message, conv, mail);

        } catch (Exception e) {
            LOG.error("Was unable to detect bank account number in msg " + message.getId(), e);
            return Collections.emptyList();

        } catch (Throwable e) {
            LOG.error("Could not process message with id " + message.getId(), e);
            return Collections.emptyList();
        }
    }

    private MailAndMessageInConversation findFirstWithBan(String ban, Mail currentMail, Message currentMessage, Conversation conv, List<Message> precedingMessages) throws Exception {
        // Iterate through all preceding messages (thus skipping last/current message),
        // find the first to contain the bank account number of the match
        for (int i = 0; i < precedingMessages.size(); i++) {
            Message precedingMessage = precedingMessages.get(i);
            Mail precedingMail = loadMail(precedingMessage);
            List<String> mailTexts = extractMailTexts(precedingMail);
            if (!finder.containsSingleBankAccountNumber(ban, mailTexts, conv.getAdId()).isEmpty()) {
                return new MailAndMessageInConversation(precedingMail, precedingMessage, i);
            }
        }
        // Not found, then the match must be in the current message.
        return new MailAndMessageInConversation(currentMail, currentMessage, precedingMessages.size());
    }

    private String getAnonSender(Message message, Conversation conv) {
        try {
            MailAddress fromUsr = mailCloakingService.createdCloakedMailAddress(
                    message.getMessageDirection().getFromRole(), conv);
            return fromUsr.getAddress();
        } catch (Exception e) {
            return "";
        }
    }

    private String getIpFromMail(Mail mail) {
        String ip = BankAccountFilterUtil.coalesce(
                trimToNull(mail.getCustomHeaders().get("FROM-IP")),
                trimToNull(mail.getUniqueHeader("X-Originating-IP")),
                trimToNull(mail.getUniqueHeader("X-SourceIP")),
                trimToNull(mail.getUniqueHeader("X-AOL-IP")));
        return StringUtils.strip(ip, "[]");
    }

    private String getReceiverFromConversation(Message message, Conversation conv) {
        return (message.getMessageDirection() == MessageDirection.BUYER_TO_SELLER) ? conv.getSellerId() : conv.getBuyerId();
    }

    private String getSenderFromConversation(Message message, Conversation conv) {
        return (message.getMessageDirection() == MessageDirection.BUYER_TO_SELLER) ? conv.getBuyerId() : conv.getSellerId();
    }

    private boolean isFirstMailInConversation(Mail mail) {
        return mail.containsHeader(Mail.ADID_HEADER);
    }

    private Mail loadMail(Message msg) throws ParsingException {
        byte[] mailAsBytes = mailRepository.readInboundMail(msg.getId());
        return mails.readMail(mailAsBytes);
    }

    private void makeSureOnly1HighestMatchGivesAScore(List<BankAccountMatch> matches) {
        int highestScore = 0;
        for (BankAccountMatch match : matches) {
            highestScore = Math.max(highestScore, match.getScore());
        }
        boolean highestScoreSeen = false;
        for (int i = 0, matchesSize = matches.size(); i < matchesSize; i++) {
            BankAccountMatch match = matches.get(i);
            if (match.getScore() < highestScore || highestScoreSeen) {
                matches.set(i, match.withZeroScore());
            }
            highestScoreSeen = highestScoreSeen || (match.getScore() == highestScore);
        }
    }

    private String toDescription(Conversation conv, BankAccountMatch match, Mail firstMailWithAccount, Message firstMessageWithAccount, int mailMatchCount) {
        String fraudsterEmail = getSenderFromConversation(firstMessageWithAccount, conv);
        int score = match.getScore();
        String bankAccountNumber = match.getBankAccount();
        String fraudsterEmailAnon = getAnonSender(firstMessageWithAccount, conv);
        String fraudsterActualEmail = trimToEmpty(firstMailWithAccount.getFrom());
        if (fraudsterActualEmail.equals(fraudsterEmail)) {
            fraudsterActualEmail = "";
        }
        String ip = trimToEmpty(getIpFromMail(firstMailWithAccount));
        String victimEmail = getReceiverFromConversation(firstMessageWithAccount, conv);
        String conversationId = conv.getId();

        return StringUtils.join(Arrays.asList(
                fraudsterEmail,
                String.valueOf(score),
                bankAccountNumber,
                fraudsterEmailAnon,
                fraudsterActualEmail,
                ip,
                victimEmail,
                String.valueOf(conversationId),
                String.valueOf(mailMatchCount)
        ), "|");
    }

    private static class MailAndMessageInConversation {
        public Mail mail;
        public Message message;
        public int indexInConversation;

        private MailAndMessageInConversation(Mail mail, Message message, int indexInConversation) {
            this.mail = mail;
            this.message = message;
            this.indexInConversation = indexInConversation;
        }
    }
}
