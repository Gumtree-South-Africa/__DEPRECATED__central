package com.ebay.ecg.bolt.api.server.push;

import com.ebay.ecg.bolt.api.server.push.model.Error;
import com.ebay.ecg.bolt.api.server.push.model.NotificationType;
import com.ebay.ecg.bolt.api.server.push.model.PushProvider;
import com.ebay.ecg.bolt.api.server.push.model.RegistrationRequest;
import com.ebay.ecg.bolt.platform.module.push.persistence.entity.PushRegistration;
import com.ebay.ecg.bolt.platform.module.push.persistence.entity.PwaDetails;
import com.ebay.ecg.bolt.platform.module.push.persistence.repository.PushServiceRepository;
import com.ebay.ecg.bolt.platform.shared.entity.common.LocaleEntity;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static java.lang.String.format;

@Controller
public class DeviceRegistrationController {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceRegistrationController.class);

    private static final List<String> SUPPORTED_LOCALES = Arrays.asList("es_MX", "es_MX_VNS", "en_ZA", "en_SG","es_AR");

    @Autowired
    private PushServiceRepository repository;

    @RequestMapping(value = "/{userId}/{pushProvider}/{notificationType}/{deviceId}/{locale}/registration", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> createUserDeviceRegistration(
      @PathVariable("userId") final String userId,
      @PathVariable("pushProvider") final String pushProvider,
      @PathVariable("notificationType") final String notificationType,
      @PathVariable("deviceId") final String deviceId,
      @PathVariable("locale") final String localeString) throws Exception {
        LOG.info("createUserDeviceRegistration for "+
          " userId = " + userId +
          " pushProvider = " + pushProvider +
          " notificationType = " + notificationType +
          " deviceId = " + deviceId +
          " locale = "+ localeString);

        LocaleEntity localeEntity;

        try {
            localeEntity = validate(pushProvider, notificationType, localeString);
        } catch (Exception e) {
            return new ResponseEntity<>(new Error(e.getMessage()), HttpStatus.BAD_REQUEST);
        }

        try { // Fix for BOLT-20886
            if (pushProvider.equalsIgnoreCase(PushProvider.gcm.name())) {
                repository.remove(userId, notificationType, PushProvider.mdns.name(), localeEntity.getLocale());
                repository.remove(userId, notificationType, PushProvider.apns.name(), localeEntity.getLocale());
            }

            PushRegistration registration = repository.find(userId, notificationType, PushProvider.valueOf(pushProvider), null,localeEntity.getLocale());

            Date currentDate = new Date(System.currentTimeMillis());

            if (registration != null) { // Update
                if (registration.getDeviceTokens() != null) {
                    if (!registration.getDeviceTokens().contains(deviceId)) {
                        registration.getDeviceTokens().add(deviceId);
                        registration.setModificationDate(currentDate);

                        repository.save(registration);

                        LOG.info("Device added to the list of existing registered devices for the user {}", userId);
                    } else {
                        LOG.warn("Registration already exists for the device id {}", deviceId);
                    }
                }
            } else { // Create
                registration = new PushRegistration();

                registration.setRegisterUserId(userId);
                registration.setCreationDate(currentDate);
                registration.setModificationDate(currentDate);
                registration.setNotificationType(notificationType);
                registration.setPushProvider(PushProvider.valueOf(pushProvider).name());
                registration.setLocale(localeEntity.getLocale());

                registration.setDeviceTokens(Collections.singletonList(deviceId));

                repository.save(registration);

                LOG.info("New user device registration record created for the user {}", userId);
            }
        } catch (RuntimeException e) {
            LOG.error("Error while creating User Device Registration", e);

            return new ResponseEntity<>(new Error(format("Error while creating User Device Registration %s", e.getMessage())), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(userId, HttpStatus.CREATED);
    }

    private LocaleEntity validate(String pushProvider, String notificationType, String localeString) throws Exception {
        LocaleEntity localeEntity;

        if (!EnumUtils.isValidEnum(PushProvider.class, pushProvider)) {
            LOG.error("Invalid Push Provider {}", pushProvider);

            throw new Exception("Invalid Push Provider");
        }

        if (!EnumUtils.isValidEnum(NotificationType.class, notificationType)) {
            LOG.error("Invalid Notification Type {}", notificationType);

            throw new Exception("Invalid Notification Type");
        }

        try { // Fix for BOLT-20181
            localeEntity = validateLocale(localeString);
        } catch (Exception ex){
            LOG.error("Unsupported locale {}", localeString);

            throw new Exception(format("Unsupported locale %s", localeString));
        }

        if (!SUPPORTED_LOCALES.contains(localeEntity.toString())) {
            LOG.error("Unsupported locale {}", localeEntity);

            throw new Exception(format("Unsupported locale %s", localeEntity));
        }

        return localeEntity;
    }

    private LocaleEntity validateLocale(final String localeString) {
        LocaleEntity localeEntity;

        String[] parts = StringUtils.tokenizeToStringArray(localeString, "_ ", false, false);

        String language = (parts.length > 0 ? parts[0] : "");
        String country = (parts.length > 1 ? parts[1] : "");
        String variant = "";

        if (parts.length > 2) {
            int endIndexOfCountryCode = localeString.lastIndexOf(country) + country.length();

            variant = StringUtils.trimLeadingWhitespace(localeString.substring(endIndexOfCountryCode));

            if (variant.startsWith("_")) {
                variant = StringUtils.trimLeadingCharacter(variant, '_').toUpperCase();
            }
        }

        if (variant.length() > 0) {
            localeEntity = new LocaleEntity(language + "_" + country + "_" + variant);
        } else {
            localeEntity = new LocaleEntity(localeString);
        }

        return localeEntity;
    }

    @RequestMapping(value = "/{userId}/{pushProvider}/{notificationType}/{deviceId}/{locale}/registration", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<?> deleteUserDeviceRegistration(
      @PathVariable("userId") final String userId,
      @PathVariable("pushProvider") final String pushProvider,
      @PathVariable("notificationType") final String notificationType,
      @PathVariable("deviceId") final String deviceId,
      @PathVariable("locale") final String localeString) throws Exception {
        LOG.info("deleteUserDeviceRegistration for "+
          " userId = " + userId +
          " pushProvider = " + pushProvider +
          " notificationType = " + notificationType +
          " deviceId = " + deviceId +
          " Locale = " + localeString);

        LocaleEntity localeEntity;

        try {
            localeEntity = validate(pushProvider, notificationType, localeString);
        } catch (Exception e) {
            return new ResponseEntity<>(new Error(e.getMessage()), HttpStatus.BAD_REQUEST);
        }

        try{
            PushRegistration registration = repository.find(userId, notificationType, PushProvider.valueOf(pushProvider), null, localeEntity.getLocale());

            if (registration != null) {
                removeDeviceToken(deviceId, registration);
            }
        } catch (RuntimeException e) {
            LOG.error("Error while deleting User Device Registration", e);

            return new ResponseEntity<>(new Error("Error while deleting User Device Registration"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(userId, HttpStatus.OK);
    }

    @RequestMapping(value = "/registrations", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> deviceRegistration(@RequestBody RegistrationRequest request) throws Exception {
        LOG.debug("DeviceRegistration for {}", request);

        LocaleEntity localeEntity;

        try {
            localeEntity = validate(request.getPushProvider().name(), request.getNotificationType().name(), request.getLocale());
        } catch (Exception e) {
            return new ResponseEntity<>(new Error(e.getMessage()), HttpStatus.BAD_REQUEST);
        }

        boolean isPwaRequest = isPwaRequest(request);

        if (isPwaRequest && !isValidPwaRequest(request)) {
            LOG.error("Invalid PWA registartion request");

            return new ResponseEntity<>(new Error("Invalid PWA registration request"), HttpStatus.BAD_REQUEST);
        }

        if (!isPwaRequest && !StringUtils.hasText(request.getDeviceId())) {
            LOG.error("Empty Device Id");

            return new ResponseEntity<>(new Error("Empty Device Id"), HttpStatus.BAD_REQUEST);
        }

        try {
            PushRegistration registration = repository.find(request.getUserId(),
              request.getNotificationType().name(),
              request.getPushProvider(),
              request.getAppType(),
              localeEntity.getLocale());

            if (registration != null) {
                if (isPwaRequest) {
                    updatePwaRegistration(request, registration);
                } else {
                    updateAppRegistration(request, registration);
                }
            } else {
                createDeviceRegistration(request, localeEntity, isPwaRequest);
            }
        } catch (RuntimeException e) {
            LOG.error("Error while creating PWA Device Registration", e);

            return new ResponseEntity<>(new Error("Error while creating PWA Device Registration"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(request.getUserId(), HttpStatus.CREATED);
    }

    // if the device is already registered, update with new public/secret keys
    private void updatePwaRegistration(RegistrationRequest request, PushRegistration registration) {
        boolean matchFound = false;

        for(int i = 0; i < registration.getPwaDetails().size(); i++) {
            if (registration.getPwaDetails().get(i).getEndPoint().equalsIgnoreCase(request.getEndPoint())) {
                matchFound = true;

                registration.getPwaDetails().get(i).setPublicKey(request.getPublicKey());
                registration.getPwaDetails().get(i).setSecret(request.getSecret());

                break;
            }
        }

        if (matchFound) {
            registration.setModificationDate(new Date(System.currentTimeMillis()));

            repository.save(registration);

            LOG.info("PWA registration updated with {}", request);
        } else {
            PwaDetails pwaDetails = new PwaDetails();

            pwaDetails.setPublicKey(request.getPublicKey());
            pwaDetails.setSecret(request.getSecret());
            pwaDetails.setEndPoint(request.getEndPoint());

            registration.getPwaDetails().add(pwaDetails);
            registration.setModificationDate(new Date(System.currentTimeMillis()));

            repository.save(registration);

            LOG.info("New PWA registration {} added to the existing registration", request);
        }
    }

    private void updateAppRegistration(RegistrationRequest request, PushRegistration registration) {
        if (!registration.getDeviceTokens().contains(request.getDeviceId())) {
            registration.getDeviceTokens().add(request.getDeviceId());
            registration.setModificationDate(new Date(System.currentTimeMillis()));

            repository.save(registration);

            LOG.info("Existing APP registration updated with {}", request);
        } else {
            LOG.warn("Registration already exists for {}", request);
        }
    }

    private void createDeviceRegistration(RegistrationRequest request, LocaleEntity localeEntity, boolean isPwaRequest) {
        PushRegistration registration = new PushRegistration();

        registration.setRegisterUserId(request.getUserId());
        registration.setCreationDate(new Date(System.currentTimeMillis()));
        registration.setModificationDate(new Date(System.currentTimeMillis()));
        registration.setNotificationType(request.getNotificationType().name());
        registration.setPushProvider(request.getPushProvider().name());
        registration.setLocale(localeEntity.getLocale());

        if (isPwaRequest) {
            registration.setAppType(request.getAppType());

            PwaDetails pwaInfo = new PwaDetails();

            pwaInfo.setPublicKey(request.getPublicKey());
            pwaInfo.setSecret(request.getSecret());
            pwaInfo.setEndPoint(request.getEndPoint());

            List<PwaDetails> pwaDetails = new ArrayList<>();

            pwaDetails.add(pwaInfo);

            registration.setPwaDetails(pwaDetails);
        } else { // app registration
            registration.setDeviceTokens(Collections.singletonList(request.getDeviceId()));
        }

        repository.save(registration);

        LOG.info("New {} registration created for {}", isPwaRequest ? "PWA" : "APP", request);
    }

    private boolean isPwaRequest(RegistrationRequest request) {
        return StringUtils.hasText(request.getAppType()) &&
          request.getAppType().equalsIgnoreCase("pwa");
    }

    private boolean isValidPwaRequest(RegistrationRequest request) {
        return StringUtils.hasText(request.getAppType())
          && request.getAppType().equalsIgnoreCase("pwa")
          && StringUtils.hasText(request.getPublicKey())
          && StringUtils.hasText(request.getSecret())
          && StringUtils.hasText(request.getEndPoint());
    }

    @RequestMapping(value = "/registrations", method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<?> deleteDeviceRegistration(@RequestBody RegistrationRequest request) throws Exception {
        LOG.info("Delete DeviceRegistration for {}", request);

        LocaleEntity localeEntity;

        try {
            localeEntity = validate(request.getPushProvider().name(), request.getNotificationType().name(), request.getLocale());
        } catch (Exception e) {
            return new ResponseEntity<>(new Error(e.getMessage()), HttpStatus.BAD_REQUEST);
        }

        try {
            PushRegistration registration = repository.find(request.getUserId(), request.getNotificationType().name(), request.getPushProvider(), request.getAppType(), localeEntity.getLocale());

            if (registration != null) {
                if (StringUtils.hasText(request.getAppType())){
                    removePwaDetails(request.getEndPoint(), registration);
                } else {
                    removeDeviceToken(request.getDeviceId(), registration);
                }
            }
        } catch (RuntimeException e) {
            LOG.error("Error while deleting User Device Registration", e);

            return new ResponseEntity<>(new Error("Error while deleting User Device Registration"), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(request.getUserId(), HttpStatus.OK);
    }

    private void removeDeviceToken(String deviceId, PushRegistration registration) {
        if (registration != null && !CollectionUtils.isEmpty(registration.getDeviceTokens()) && registration.getDeviceTokens().contains(deviceId)) {
            registration.getDeviceTokens().remove(deviceId);

            if (registration.getDeviceTokens().isEmpty()) {
                repository.remove(registration.getId());

                LOG.info("APP registration deleted for the user {}", registration.getRegisterUserId());
            } else {
                repository.save(registration);

                LOG.info("APP registration updated to remove device id for the user {}", registration.getRegisterUserId());
            }
        }
    }

    private void removePwaDetails(String endpoint, PushRegistration registration) {
        boolean matchFound = false;

        if (registration != null && !CollectionUtils.isEmpty(registration.getPwaDetails())) {
            for (int i = 0; i < registration.getPwaDetails().size(); i++) {
                if (registration.getPwaDetails().get(i).getEndPoint().equalsIgnoreCase(endpoint)) {
                    registration.getPwaDetails().remove(i);

                    matchFound = true;

                    break;
                }
            }

            if (matchFound) {
                if (registration.getPwaDetails().isEmpty()) {
                    repository.remove(registration.getId());

                    LOG.info("PWA registration deleted for the user {}", registration.getRegisterUserId());
                } else {
                    repository.save(registration);

                    LOG.info("PWA registration updated to remove end point for the user {}", registration.getRegisterUserId());
                }
            }
        }

        if (!matchFound) {
            LOG.info("No PWA registration found for the end point {}", endpoint);
        }
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, NullPointerException.class })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public String handleBadRequest(Exception e) {
        LOG.info("Bad request. Reason is  ::: {}", e.getMessage());

        return e.getMessage();
    }
}