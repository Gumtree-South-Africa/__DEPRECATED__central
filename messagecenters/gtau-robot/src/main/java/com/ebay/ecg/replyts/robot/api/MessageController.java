package com.ebay.ecg.replyts.robot.api;

import com.ebay.ecg.replyts.robot.api.requests.payload.GetConversationsResponsePayload;
import com.ebay.ecg.replyts.robot.service.RobotService;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.StringArrayPropertyEditor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class MessageController {

    private final RobotService robotService;

    @Autowired
    public MessageController(RobotService robotService) {
        this.robotService = robotService;
    }

    @InitBinder
    public void initBinderInternal(WebDataBinder binder) {
        binder.registerCustomEditor(String[].class, new StringArrayPropertyEditor());
    }

    @GetMapping("/users/{email}/ads/{adId}")
    public ResponseObject<GetConversationsResponsePayload> getConversationsByAdIdAndEmail(@PathVariable String email, @PathVariable String adId) {
        return ResponseObject.of(robotService.getConversationsForAd(email, adId));
    }
}
