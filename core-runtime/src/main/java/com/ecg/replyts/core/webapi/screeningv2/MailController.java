package com.ecg.replyts.core.webapi.screeningv2;

import com.ecg.replyts.core.api.persistence.MailRepository;
import com.ecg.replyts.core.api.webapi.commands.GetRawMailCommand;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.api.webapi.model.MailTypeRts;
import com.ecg.replyts.core.webapi.screeningv2.converter.DomainObjectConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletResponse;

/**
 * gives access to the mails ReplyTS received and sent out. Mails that were received are called inbound, mails that
 * were sent out, are called outbound.
 */
@Controller
@ConditionalOnExpression("'${persistence.strategy}' == 'riak' || '${persistence.strategy}'.startsWith('hybrid')")
public class MailController {
    private final MailRepository mailRepository;

    private static final Logger LOG = LoggerFactory.getLogger(MailController.class);

    @Autowired
    MailController(MailRepository mailRepository) {
        this.mailRepository = mailRepository;
    }

    /**
     * Gets the raw contents of a given mail - the response will not be of type application/json but rather the actual
     * mail contents.
     */
    @RequestMapping(value = GetRawMailCommand.MAPPING, produces = "application/download")
    @ResponseStatus(HttpStatus.OK)
    void getRawMail(
            HttpServletResponse response,
            @PathVariable("messageId") String messageId,
            @PathVariable("mailType") MailTypeRts type
    ) throws Exception {

        byte[] mailContents;

        switch (type) {
            case OUTBOUND:
                mailContents = mailRepository.readOutboundMail(messageId);
                break;
            case INBOUND:
            default:
                mailContents = mailRepository.readInboundMail(messageId);
                break;
        }

        if (mailContents == null) {
            LOG.warn("Did not find content for messsageid {} ", messageId);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        } else {
            response.getOutputStream().write(mailContents);
        }
        response.getOutputStream().close();
    }


}
