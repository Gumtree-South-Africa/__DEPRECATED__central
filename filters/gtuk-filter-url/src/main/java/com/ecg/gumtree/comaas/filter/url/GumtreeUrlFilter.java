package com.ecg.gumtree.comaas.filter.url;

import com.ecg.gumtree.comaas.common.filter.GumtreeFilterUtil;
import com.ecg.gumtree.replyts2.common.message.GumtreeCustomHeaders;
import com.ecg.replyts.core.api.model.conversation.Conversation;
import com.ecg.replyts.core.api.model.conversation.Message;
import com.ecg.replyts.core.api.model.mail.Mail;
import com.ecg.replyts.core.api.model.mail.TypedContent;
import com.ecg.replyts.core.api.pluginconfiguration.filter.FilterFeedback;
import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import com.gumtree.filters.comaas.Filter;
import com.gumtree.filters.comaas.config.UrlFilterConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ecg.gumtree.comaas.common.filter.GumtreeFilterUtil.longDescription;
import static com.ecg.gumtree.comaas.common.filter.GumtreeFilterUtil.resultFilterResultMap;

@Component
public class GumtreeUrlFilter implements com.ecg.replyts.core.api.pluginconfiguration.filter.Filter {
    private static final Logger LOG = LoggerFactory.getLogger(GumtreeUrlFilter.class);

    private static final Pattern EMAIL_ADDR_PATTERN =
            Pattern.compile("\\b[A-Z0-9._%+-]{1,64}@(?:[A-Z0-9-]{1,63}\\.){1,8}[A-Z]{2,63}\\b",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern URL_PATTERN =
            Pattern.compile("(?:(?:(?:\\b|[a-z])(?:https?|ftp|file)(?:://|:/|/))|\\s|\"|>|^)(?:(?:[a-zA-Z0-9-](?:(?:\\%[0-9A-F]{2}){1,})?){1,63}\\.){1,8}[A-Z]{2,63}(?:(/|:\\d+[/\\s<]|\\s|\"|<|$))",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern COMMON_PHONE_NUM_PATTERN =
            Pattern.compile("(?:\\.|\\s|^)07\\d{9}\\.");
    private static final Pattern COMMON_URL_EXTS_PATTERN = Pattern.compile("^(com|net|org|edu|gov|biz|xxx|info|uk|ru|xyz|)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern URL_START_PATTERN = Pattern.compile("(https?|ftp|file)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final String KEY_STRIPPED_MAILS = GumtreeUrlFilter.class.getName() + ":STRIPPED-MAILS";
    private static final String PRO_USER_HEADER_VALUE = "ACCOUNT_HOLDER";
    private static final String SHORT_DESCRIPTION = "Disallowed url detected";

    private Filter pluginConfig;
    private UrlFilterConfig filterConfig;

    @Override
    public List<FilterFeedback> filter(MessageProcessingContext messageContext) {
        Message message = messageContext.getMessage();
        Conversation conversation = messageContext.getConversation();
        if (!isFirstMessage(message, conversation)) {
            return Collections.emptyList();
        }

        if (GumtreeFilterUtil.hasExemptedCategory(filterConfig, messageContext)) {
            return Collections.emptyList();
        }

        List<String> textsToCheck = getTextsToCheck(messageContext);
        return checkTexts(messageContext, textsToCheck);
    }

    private boolean isFirstMessage(Message message, Conversation conversation) {
        return message.getId().equals(conversation.getMessages().iterator().next().getId());
    }

    private List<FilterFeedback> checkTexts(MessageProcessingContext messageContext, List<String> textsToCheck) {
        List<FilterFeedback> reasons = new ArrayList<>();

        List<String> urlsToCheck = findEmailAddrsInContent(textsToCheck);
        urlsToCheck.addAll(findUrlsInContent(textsToCheck));

        // As soon as an unsafe URL (or e-mail address) is found, stop
        for (String url : urlsToCheck) {
            if (!isUrlSafe(url, messageContext)) {
                LOG.debug("Found unsafe url: (" + url + ")");
                String description = longDescription(this.getClass(), pluginConfig.getInstanceId(), filterConfig.getVersion(), SHORT_DESCRIPTION);
                reasons.add(new FilterFeedback(url, description, 0, resultFilterResultMap.get(filterConfig.getResult())));
                break; // Don't check the other parts of the mail
            }
        }

        return reasons;
    }

    private List<String> findEmailAddrsInContent(List<String> contents) {
        List<String> emailAddrs = new ArrayList<>();

        for (String plainTextPart : contents) {
            Matcher m = EMAIL_ADDR_PATTERN.matcher(plainTextPart);
            int findIndex = 0;

            while (m.find(findIndex)) {
                String emailAddr = m.group();
                findIndex = m.end();
                emailAddrs.add(StringUtils.strip(emailAddr, " <>\n"));
            }
        }

        return emailAddrs;
    }

    // Eliminate matches that are unlikely to be URLs based on data obtained from
    // Gumshield trends. People often put dots around their mobile number
    // The other common issue is what appear to be URLs of form a.b but are really
    // just people not putting a space after full stop.
    private List<String> eliminateUnlikelyUrls(List<String> urls) {
        Iterator<String> iterator = urls.iterator();

        while (iterator.hasNext()) {
            String url = iterator.next();
            if (COMMON_PHONE_NUM_PATTERN.matcher(url).find()) {
                iterator.remove();
            } else {
                String urlParts[] = url.split("\\.");
                if (urlParts.length == 2) {
                    if (!isUrl(urlParts[0], urlParts[1])) {
                        LOG.debug("URL Filter found {} in message, but concluded that it might not actually be a URL", url);
                        iterator.remove();
                    }
                }
            }
        }
        return urls;
    }

    private boolean isUrl(String firstPart, String secondPart) {
        return URL_START_PATTERN.matcher(firstPart).find() || secondPart.endsWith("/") || (COMMON_URL_EXTS_PATTERN.matcher(secondPart).matches());
    }

    private List<String> findUrlsInContent(List<String> textsToCheck) {
        List<String> urls = new ArrayList<>();

        for (String plainTextPart : textsToCheck) {
            Matcher m = URL_PATTERN.matcher(plainTextPart);
            int findIndex = 0;

            while (m.find(findIndex)) {
                String url = m.group();
                findIndex = m.end();
                urls.add(StringUtils.strip(url, " <>\n"));
            }
        }

        return eliminateUnlikelyUrls(urls);
    }

    private List<String> getTextsToCheck(MessageProcessingContext messageProcessingContext) {
        List<String> textsToCheck = new ArrayList<>();
        try {
            // Note: we do not want to check the subject
            List<String> texts = loadTextParts(messageProcessingContext);
            if (texts != null && !texts.isEmpty()) {
                textsToCheck.addAll(texts);
            }
        } catch (Exception e) {
            LOG.warn("There was an exception when trying to load the texts to check from message "
                    + messageProcessingContext.getMessage().getId()
                    + ". Can't filter them", e);
        }

        return textsToCheck;
    }

    private List<String> loadTextParts(MessageProcessingContext messageProcessingContext) {
        Mail mail = messageProcessingContext.getMail();
        Object textParts = messageProcessingContext.getFilterContext().get(KEY_STRIPPED_MAILS);
        if (textParts != null && textParts instanceof List<?>) {
            //noinspection unchecked
            return (List<String>) textParts;
        } else {
            List<String> ptParts = new ArrayList<>();

            List<TypedContent<String>> contents = mail.getTextParts(false);

            for (TypedContent<String> content : contents) {
                ptParts.add(content.getContent());
            }

            messageProcessingContext.getFilterContext().put(KEY_STRIPPED_MAILS, ptParts);
            return ptParts;
        }
    }

    private boolean isUrlSafe(String url, MessageProcessingContext messageContext) {
        List<String> safeUrls = filterConfig.getSafeUrls();

        if (getSellerIsPro(messageContext) && url.equals(getBuyerId(messageContext))) {
            return true;
        }

        for (String safeUrl : safeUrls) {
            String urlNoProtocol = url.toLowerCase().replaceFirst("(https?|ftp|file)://", "");
            int safeIndex = urlNoProtocol.indexOf(safeUrl.toLowerCase());
            int slashIndex = urlNoProtocol.indexOf("/");

            if (safeIndex > -1 && (slashIndex == -1 || safeIndex < slashIndex)) {
                return true;
            }
        }

        return false;
    }

    private String getBuyerId(MessageProcessingContext messageContext) {
        return messageContext.getConversation().getBuyerId();
    }

    private Boolean getSellerIsPro(MessageProcessingContext messageContext) {
        if (messageContext.getMessage().getHeaders().containsKey(GumtreeCustomHeaders.SELLER_IS_PRO.getHeaderValue())) {
            return "true".equals(messageContext.getMessage().getHeaders().get(GumtreeCustomHeaders.SELLER_IS_PRO.getHeaderValue()));
        } else {
            return PRO_USER_HEADER_VALUE.equals(getSellerGoodHeaderValue(messageContext));
        }
    }

    private String getSellerGoodHeaderValue(MessageProcessingContext messageContext) {
        return messageContext.getMessage().getHeaders().get(GumtreeCustomHeaders.SELLER_GOOD.getHeaderValue());
    }

    GumtreeUrlFilter withPluginConfig(Filter pluginConfig) {
        this.pluginConfig = pluginConfig;
        return this;
    }

    GumtreeUrlFilter withFilterConfig(UrlFilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        return this;
    }
}
