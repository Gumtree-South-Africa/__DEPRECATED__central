package nl.marktplaats.postprocessor.anonymizebody.safetymessage.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by reweber on 19/10/15
 */
public class PlainTextMailPartInsertion implements SafetyTextInsertion {

    private static final Logger LOG = LoggerFactory.getLogger(PlainTextMailPartInsertion.class);

    /** {@inheritDoc} */
    @Override
    public String insertSafetyText(String content, String safetyText) {
        LOG.debug("Inserting safety text in plain text part.");
        return convertToPlain(safetyText) + "\r\n\r\n" + content;
    }

    private String convertToPlain(String textToInsert) {
        return textToInsert
                .replaceAll("<br ?/?>", "\r\n")
                .replaceAll("<a href=\"mailto:([^\"]*)\">.*?</a>", "$1")
                .replaceAll("<a href=\"([^\"]*)\">.*?</a>", "$1")
                .replaceAll("<[sS][tT][yY][lL][eE]>[^<]*</[sS][tT][yY][lL][eE]>", "")
                .replace("&nbsp;", " ")
                .replaceAll("<[^>]*>", "");
    }
}
