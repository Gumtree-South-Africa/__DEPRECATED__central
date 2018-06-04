package com.ecg.messagecenter.kjca.webapi;

import com.ecg.messagecenter.kjca.sync.BlockService;
import com.ecg.messagecenter.kjca.sync.WebApiSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class BlockController {

    private static final Logger LOG = LoggerFactory.getLogger(BlockController.class);

    private final BlockService blockService;
    private final boolean syncEnabled;

    @Autowired(required = false)
    private WebApiSyncService webapiSyncService;

    @Autowired
    public BlockController(
            BlockService blockService,
            @Value("${webapi.sync.ca.enabled:false}") boolean syncEnabled) {

        this.blockService = blockService;
        this.syncEnabled = syncEnabled;

        if (syncEnabled) {
            LOG.info(this.getClass().getSimpleName() + " runs in SyncMode");
        }
    }

    @PostMapping("/postboxes/{email}/conversations/{conversationId}/block")
    public ResponseEntity<Void> blockConversation(
            @PathVariable("email") final String email,
            @PathVariable("conversationId") final String conversationId) {

        boolean result = syncEnabled
                ? webapiSyncService.blockConversation(email, conversationId)
                : blockService.blockConversation(email, conversationId);

        if (result) {
            return ResponseEntity.accepted().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/postboxes/{email}/conversations/{conversationId}/block")
    public ResponseEntity<Void> unblockConversation(
            @PathVariable("email") final String email,
            @PathVariable("conversationId") final String conversationId) {

        boolean result = syncEnabled
                ? webapiSyncService.unblockConversation(email, conversationId)
                : blockService.unblockConversation(email, conversationId);

        if (result) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
