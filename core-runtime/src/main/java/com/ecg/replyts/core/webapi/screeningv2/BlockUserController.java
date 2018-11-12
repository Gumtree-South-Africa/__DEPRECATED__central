package com.ecg.replyts.core.webapi.screeningv2;

import com.ecg.replyts.app.UserEventListener;
import com.ecg.replyts.core.api.model.user.event.BlockedUserEvent;
import com.ecg.replyts.core.api.webapi.envelope.RequestState;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.ecg.replyts.core.runtime.persistence.BlockUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.ecg.replyts.core.api.model.user.event.BlockAction.BLOCK_USER;
import static com.ecg.replyts.core.api.model.user.event.BlockAction.UNBLOCK_USER;

@RestController
@RequestMapping(value = "/block-users", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class BlockUserController {
    private static final Logger LOG = LoggerFactory.getLogger(BlockUserController.class);

    @Autowired
    private BlockUserRepository blockUserRepository;

    @Autowired(required = false)
    private UserEventListener userEventListener;

    @PostMapping("/{blockerId}/{blockeeId}")
    ResponseObject<?> blockUser(@PathVariable String blockerId, @PathVariable String blockeeId) {
        LOG.trace("Blocking user, blockerId: %s blockeeId: %s", blockerId, blockeeId);

        blockUserRepository.blockUser(blockerId, blockeeId);

        if (userEventListener != null) {
            userEventListener.eventTriggered(new BlockedUserEvent(blockerId, blockeeId, BLOCK_USER));
        }

        return ResponseObject.of(RequestState.OK);
    }

    @DeleteMapping("/{blockerId}/{blockeeId}")
    ResponseObject<?> unblockUser(@PathVariable String blockerId, @PathVariable String blockeeId) {
        LOG.trace("Unblocking user, blockerId: %s blockeeId: %s", blockerId, blockeeId);

        blockUserRepository.unblockUser(blockerId, blockeeId);

        if (userEventListener != null) {
            userEventListener.eventTriggered(new BlockedUserEvent(blockerId, blockeeId, UNBLOCK_USER));
        }

        return ResponseObject.of(RequestState.OK);
    }

    @GetMapping("/{blockerId}/{blockeeId}")
    boolean isblocked(@PathVariable String blockerId, @PathVariable String blockeeId) {
        return blockUserRepository.hasBlocked(blockerId, blockeeId);
    }

    @GetMapping("/{blockerId}")
    List<String> listBlockedUsers(@PathVariable String blockerId) {
        return blockUserRepository.listBlockedUsers(blockerId);
    }
}
