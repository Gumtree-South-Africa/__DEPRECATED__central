package com.ebay.ecg.bolt.api.server.push;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.ebay.ecg.bolt.api.server.push.model.Error;
import com.ebay.ecg.bolt.api.server.push.model.Meta;
import com.ebay.ecg.bolt.api.server.push.model.NotificationRequest;
import com.ebay.ecg.bolt.api.server.push.model.NotificationType;
import com.ebay.ecg.bolt.domain.service.push.PushDomainService;
import com.ebay.ecg.bolt.domain.service.push.model.PushMessagePayload;
import com.ebay.ecg.bolt.domain.service.push.model.Result;
import com.ebay.ecg.bolt.platform.shared.entity.common.LocaleEntity;

import static java.lang.String.format;

@Controller
public class PushNotificationController {
    private static final Logger LOG = LoggerFactory.getLogger(PushNotificationController.class);

    @Autowired
    private PushDomainService pushDomainService;

    @RequestMapping(value = "/{receiverUserId}/{notificationType}/{locale}/notifications", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> pushNotifications(
      @PathVariable("receiverUserId") final String receiverUserId,
      @PathVariable("notificationType") final String notificationType,
      @PathVariable("locale") final LocaleEntity localeEntity,
      @RequestBody NotificationRequest notificationRequest) throws Exception {
        String alertId = null;
        String conversationId = null;
        String notificationId = null;

        try {
            Assert.notNull(notificationRequest);
            Assert.notNull(notificationRequest.getMessage());
            Assert.notNull(notificationRequest.getToEmail());
            Assert.notNull(notificationRequest.getMeta());

            if (NotificationType.CHATMESSAGE.name().equals(notificationType)) {
                conversationId = notificationRequest.getMeta().getConversationId();

                Assert.notNull(notificationRequest.getMeta().getAdId());
                Assert.notNull(notificationRequest.getMeta().getAdTitle());
                Assert.notNull(conversationId);
            } else if (NotificationType.SEARCHALERTS.name().equals(notificationType)) {
                alertId = notificationRequest.getMeta().getAlertId();
                notificationId = notificationRequest.getMeta().getNotificationId();

                Assert.notNull(alertId);
            }
        } catch (IllegalArgumentException e){
            LOG.error("Error in the request data", e);

            return new ResponseEntity<>(new Error("Invalid Request"), HttpStatus.BAD_REQUEST);
        }

        LOG.info("PushNotifications for userId = " + receiverUserId +
          " notificationType = " + notificationType +
          " conversation id = "+ conversationId +
          " alert id = " + alertId +
          " notification id = " + notificationId +
          " ad id = "+notificationRequest.getMeta().getAdId()+
          " sender id = "+notificationRequest.getMeta().getSenderId());

        // validation for the notification type
        if (!EnumUtils.isValidEnum(NotificationType.class, notificationType)) {
            LOG.error("Invalid Notification Type {}", notificationType);

            return new ResponseEntity<>(new Error("Invalid Notification Type"), HttpStatus.BAD_REQUEST);
        }

        try {
            Meta meta = notificationRequest.getMeta();

            Map<String, String> pushMap = new HashMap<>();

            if (!StringUtils.isEmpty(conversationId)) {
                pushMap.put("conversationId", conversationId);

            }
            if (!StringUtils.isEmpty(alertId)) {
                pushMap.put("alertId", alertId);
            }
            if (!StringUtils.isEmpty(notificationId)) {
                pushMap.put("notificationId", notificationId);
            }

            String _locale = format("%s_%s", localeEntity.getLocale().getLanguage(), localeEntity.getLocale().getCountry());

            if (localeEntity.getLocale().getVariant() !=null) {
                _locale = _locale + "_" + localeEntity.getLocale().getVariant();
            }

            pushMap.put("receiverId", meta.getReceiverUserId());
            pushMap.put("adId", meta.getAdId());
            pushMap.put("adTitle", meta.getAdTitle());
            pushMap.put("locale", _locale);
            pushMap.put("senderId", meta.getSenderId());

            if (!StringUtils.isEmpty(meta.getBadge())) {
                pushMap.put("badge", String.valueOf(meta.getBadge()));
            }

            if (!StringUtils.isEmpty(meta.getAdThumbNail())) {
                pushMap.put("adImage", meta.getAdThumbNail());
            }

            PushMessagePayload payload = new PushMessagePayload(
              notificationRequest.getToEmail(),
              notificationRequest.getMessage(),
              notificationType,
              pushMap);

            List<Result> results = pushDomainService.sendPushMessages(receiverUserId, payload,notificationRequest.getAppType());

            if (results != null && results.size() == 0) {
                return new ResponseEntity<>(results, HttpStatus.NOT_FOUND);
            } else {
                return new ResponseEntity<>(results, HttpStatus.CREATED);
            }
        } catch (Exception e) {
            LOG.error("Error while doing the push notification", e);

            return new ResponseEntity<>(new Error("Error while doing the push notification"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}