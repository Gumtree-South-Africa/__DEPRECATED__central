package nl.marktplaats.postprocessor.anonymizebody.safetymessage.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by reweber on 19/10/15
 */
public class HtmlMailPartInsertion implements SafetyTextInsertion {

    private static final Logger LOG = LoggerFactory.getLogger(HtmlMailPartInsertion.class);

    /** {@inheritDoc} */
    @Override
    public String insertSafetyText(String content, String safetyText) {
        LOG.debug("Inserting safety text in html part.");
        return "<html><p>" + convertToHtml(safetyText) + "</p><br></html>" + content;
    }

    private String convertToHtml(String textToInsert) {
        return textToInsert.replace("\n", "<br>");
    }
}
