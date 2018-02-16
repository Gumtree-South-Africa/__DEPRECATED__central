package com.ecg.messagebox.controllers;

import com.ecg.messagebox.controllers.requests.PartnerMessagePayload;
import com.ecg.messagebox.service.PostBoxService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "message.synchronizer.enabled", havingValue = "true")
public class PartnerController {

    private final PostBoxService postBoxService;

    public PartnerController(PostBoxService postBoxService) {
        this.postBoxService = postBoxService;
    }

    @PostMapping("partner-sync")
    public ResponseEntity<Void> pushPartnerMessage(@RequestBody PartnerMessagePayload payload) {
        return postBoxService.storePartnerMessage(payload)
                .map(convId -> ResponseEntity.ok().build())
                .orElse(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }
}
