package com.ecg.messagecenter.webapi;

import com.ecg.messagecenter.persistence.PostBox;
import com.ecg.messagecenter.persistence.PostBoxService;
import com.ecg.messagecenter.webapi.requests.IncreaseUnreadCountersCommand;
import com.ecg.replyts.core.api.webapi.envelope.ResponseObject;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.FluentIterable.from;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

// TODO [ taken from original mp repository ]: To be refactored !!! We currently don't use it so park it for now !!!
@Controller
class CounterIncrementController {

    private final PostBoxService postBoxService;

    private static final Logger LOG = LoggerFactory.getLogger(CounterIncrementController.class);


    @Autowired
    public CounterIncrementController(PostBoxService postBoxService) {
        this.postBoxService = postBoxService;
    }

    @ExceptionHandler
    public void handleException(Throwable ex, HttpServletResponse response, Writer writer) throws IOException {
        new TopLevelExceptionHandler(ex, response, writer).handle();
    }

    @RequestMapping(value = IncreaseUnreadCountersCommand.MAPPING, method = POST)
    @ResponseBody
    public ResponseObject<?> incrementReadCounters(@RequestBody IncreaseUnreadCountersCommand cmd) {

        Result loadedItems = preloadPostBoxes(cmd);

        boolean errors = loadedItems.errors;

        Map<String, Integer> newUnreadCounters = Maps.newLinkedHashMap();
        for (IncreaseUnreadCountersCommand.Item item : cmd.getItems()) {
            if (!loadedItems.items.containsKey(item.getUserEmailLowerCase())) {
                continue;
            }

            PostBox postBox = loadedItems.items.get(item.getUserEmailLowerCase());
            postBox.markConversationUnread(item.getConversationId(), item.getMessage());
            newUnreadCounters.put(item.getConversationId(), postBox.getUnreadConversations().size());
            LOG.debug("mark as read: '{}/{}'", item.getUserEmailLowerCase(), item.getConversationId());
        }



        for (PostBox postBox : loadedItems.items.values()) {
            try {
// TODO [ taken from original mp repository ]: implement me!
//                repository.write(postBox);
            } catch (RuntimeException e) {
                LOG.error("could not update postbox: #" + postBox.getUserId(), e);
            }
        }

        Preconditions.checkArgument(!errors, "Bulk action finished with errors. please see log for details");


        return ResponseObject.of(newUnreadCounters);
    }

    private Result preloadPostBoxes(IncreaseUnreadCountersCommand cmd) {
        // in most usecases the seller's post box *will* be referenced multiple times.
        // this is to prevent loading the same postbox more than once.
        boolean errors = false;
        Set<String> emails = from(cmd.getItems()).transform(new Function<IncreaseUnreadCountersCommand.Item, String>() {
            @Override
            public String apply(IncreaseUnreadCountersCommand.Item input) {
                return input.getUserEmailLowerCase();
            }
        }).toSet();

        Map<String, PostBox> postBoxes = Maps.newHashMap();

        for (String mailAddress : emails) {
            try {
// TODO [ taken from original mp repository ]: implement me!
                PostBox postBox = null; // postBoxService.getPostBox(mailAddress);
                if (postBox != null) {
                    postBoxes.put(mailAddress, postBox);
                }
            } catch (RuntimeException e) {
                errors = true;
                LOG.error("batch postbox loading: error with postbox #" + mailAddress, e);
            }
        }

        return new Result(ImmutableMap.copyOf(postBoxes), errors);
    }

    private class Result {
        final Map<String, PostBox> items;
        final boolean errors;

        private Result(Map<String, PostBox> items, boolean errors) {
            this.items = items;
            this.errors = errors;
        }
    }
}
