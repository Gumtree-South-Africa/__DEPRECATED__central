package com.ecg.replyts.core.webapi.configv2;

import com.ecg.replyts.core.api.util.JsonObjects;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.ecg.replyts.core.webapi.util.EchoResponseGenerator.randomEchoResponse;

@Controller
public class PingController {

    @ResponseBody
    @RequestMapping("/ping")
    public Object ping() {
        return JsonObjects.builder().attr("ping", randomEchoResponse()).build();
    }
}
