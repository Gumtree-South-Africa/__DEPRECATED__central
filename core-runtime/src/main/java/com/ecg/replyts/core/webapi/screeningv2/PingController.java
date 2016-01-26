package com.ecg.replyts.core.webapi.screeningv2;

import com.ecg.replyts.core.api.util.JsonObjects;
import com.ecg.replyts.core.api.webapi.commands.PingCommand;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.ecg.replyts.core.webapi.util.EchoResponseGenerator.randomEchoResponse;

/**
 * ping resource, monitoring tools can use to see, if ReplyTS can be accessed via the network.
 */
@Controller
class PingController {

    @ResponseBody
    @RequestMapping(PingCommand.MAPPING)
    public Object ping() {
        return JsonObjects.builder().attr("ping", randomEchoResponse()).build();
    }
}
