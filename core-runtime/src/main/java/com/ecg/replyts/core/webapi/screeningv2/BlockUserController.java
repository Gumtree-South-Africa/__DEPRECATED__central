package com.ecg.replyts.core.webapi.screeningv2;

import com.codahale.metrics.Timer;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.ecg.replyts.core.runtime.TimingReports.newTimer;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Blocks users communications
 */

@Controller
public class BlockUserController {
    private static final Timer BLOCK_USER_TIMER = newTimer("webapi.block-user");
    private static final Timer UNBLOCK_USER_TIMER = newTimer("webapi.unblock-user");

    private final BlockUserRepository blockUserRepository;

    @Autowired
    public BlockUserController(BlockUserRepository blockUserRepository){
        this.blockUserRepository = blockUserRepository;
    }

    @RequestMapping(value = "/block-users/{blockerId}/{blockeeId}", produces = APPLICATION_JSON_VALUE, method = POST)
    @ResponseBody
    ResponseObject<?> blockUser(@PathVariable String blockerId, @PathVariable String blockeeId) {
        try (Timer.Context ignored = BLOCK_USER_TIMER.time()) {
            blockUserRepository.blockUser(blockerId, blockeeId);
            return ResponseObject.of(RequestState.OK);
        }
    }

    @RequestMapping(value = "/block-users/{blockerId}/{blockeeId}", produces = APPLICATION_JSON_VALUE, method = DELETE)
    @ResponseBody
    ResponseObject<?> unblockUser(@PathVariable String blockerId, @PathVariable String blockeeId) {
        try (Timer.Context ignored = UNBLOCK_USER_TIMER.time()) {
            blockUserRepository.unblockUser(blockerId, blockeeId);
            return ResponseObject.of(RequestState.OK);
        }
    }
}
