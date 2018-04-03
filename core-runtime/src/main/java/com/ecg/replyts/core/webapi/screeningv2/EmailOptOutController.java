package com.ecg.replyts.core.webapi.screeningv2;

import com.ecg.replyts.app.UserEventListener;
import com.ecg.replyts.core.api.model.user.event.EmailPreferenceEvent;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.persistence.EmailOptOutRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.ecg.replyts.core.api.model.user.event.EmailPreferenceCommand.TURN_OFF_EMAIL;
import static com.ecg.replyts.core.api.model.user.event.EmailPreferenceCommand.TURN_ON_EMAIL;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@Controller
@ConditionalOnExpression("'${email.opt.out.enabled:false}' == 'true' && ('${persistence.strategy}' == 'cassandra' || '${persistence.strategy}'.startsWith('hybrid'))")
public class EmailOptOutController {
    private static final Logger LOG = LoggerFactory.getLogger(EmailOptOutController.class);

    @Autowired
    private EmailOptOutRepository emailOptOutRepository;

    @Autowired(required = false)
    private UserEventListener userEventListener;

    @RequestMapping(value = "/email-notifications/{userId}/turn-on", produces = APPLICATION_JSON_UTF8_VALUE, method = PUT)
    @ResponseBody
    ResponseObject<?> emailTurnOn(@PathVariable String userId) {
        LOG.trace("Turning on email notifications for userId: " + userId);

        emailOptOutRepository.turnOnEmail(userId);

        if (userEventListener != null) {
            userEventListener.eventTriggered(new EmailPreferenceEvent(TURN_ON_EMAIL, userId));
        }

        return ResponseObject.of(RequestState.OK);
    }

    @RequestMapping(value = "/email-notifications/{userId}/turn-off", produces = APPLICATION_JSON_UTF8_VALUE, method = PUT)
    @ResponseBody
    ResponseObject<?> emailTurnOff(@PathVariable String userId) {
        LOG.trace("Turning off email notifications for userId: " + userId);

        emailOptOutRepository.turnOffEmail(userId);

        if (userEventListener != null) {
            userEventListener.eventTriggered(new EmailPreferenceEvent(TURN_OFF_EMAIL, userId));
        }

        return ResponseObject.of(RequestState.OK);
    }

    @RequestMapping(value = "/email-notifications/{userId}", produces = APPLICATION_JSON_UTF8_VALUE, method = GET)
    @ResponseBody
    ResponseObject<?> isEmailTurnedOn(@PathVariable String userId) {
        final boolean emailTurnedOn = emailOptOutRepository.isEmailTurnedOn(userId);
        return ResponseObject.of(new EmailNotificationsStatus(emailTurnedOn));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    class EmailNotificationsStatus {
        public boolean isEmailNotifications() {
            return emailNotifications;
        }

        public void setEmailNotifications(boolean emailNotifications) {
            this.emailNotifications = emailNotifications;
        }

        public boolean emailNotifications;

        EmailNotificationsStatus(boolean emailNotification) {
            this.emailNotifications = emailNotification;
        }
    }
}
