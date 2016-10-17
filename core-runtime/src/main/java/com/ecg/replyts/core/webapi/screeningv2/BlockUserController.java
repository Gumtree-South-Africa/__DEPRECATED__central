package com.ecg.replyts.core.webapi.screeningv2;

import com.codahale.metrics.Timer;
import com.ecg.replyts.app.UserEventListener;
import com.ecg.replyts.core.api.model.user.event.BlockAction;
import com.ecg.replyts.core.api.model.user.event.BlockedUserEvent;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import static com.ecg.replyts.core.api.model.user.event.BlockAction.BLOCK_USER;
import static com.ecg.replyts.core.api.model.user.event.BlockAction.UNBLOCK_USER;
import static com.ecg.replyts.core.runtime.TimingReports.newTimer;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

/**
 * Blocks users communications
 */

@Controller
@ConditionalOnExpression("#{('${persistence.strategy}' == 'cassandra' || '${persistence.strategy}'.startsWith('hybrid'))}")
public class BlockUserController {
    private static final Timer BLOCK_USER_TIMER = newTimer("webapi.block-user");
    private static final Timer UNBLOCK_USER_TIMER = newTimer("webapi.unblock-user");
    private static final Logger log = LoggerFactory.getLogger(BlockUserController.class);

    private final BlockUserRepository blockUserRepository;
    private final UserEventListener userEventListener;

    @Autowired
    public BlockUserController(BlockUserRepository blockUserRepository, UserEventListener userEventListener){
        this.blockUserRepository = blockUserRepository;
        this.userEventListener = userEventListener;
    }

    @RequestMapping(value = "/block-users/{blockerId}/{blockeeId}", produces = APPLICATION_JSON_VALUE, method = POST)
    @ResponseBody
    ResponseObject<?> blockUser(@PathVariable String blockerId, @PathVariable String blockeeId) {
        try (Timer.Context ignored = BLOCK_USER_TIMER.time()) {
            log.trace("Blocking user, blockerId: %s blockeeId: %s", blockerId, blockeeId);
            blockUserRepository.blockUser(blockerId, blockeeId);
            userEventListener.eventTriggered(new BlockedUserEvent(blockerId, blockeeId, BLOCK_USER));
            return ResponseObject.of(RequestState.OK);
        }
    }

    @RequestMapping(value = "/block-users/{blockerId}/{blockeeId}", produces = APPLICATION_JSON_VALUE, method = DELETE)
    @ResponseBody
    ResponseObject<?> unblockUser(@PathVariable String blockerId, @PathVariable String blockeeId) {
        try (Timer.Context ignored = UNBLOCK_USER_TIMER.time()) {
            log.trace("Unblocking user, blockerId: %s blockeeId: %s", blockerId, blockeeId);
            blockUserRepository.unblockUser(blockerId, blockeeId);
            userEventListener.eventTriggered(new BlockedUserEvent(blockerId, blockeeId, UNBLOCK_USER));
            return ResponseObject.of(RequestState.OK);
        }
    }
}
