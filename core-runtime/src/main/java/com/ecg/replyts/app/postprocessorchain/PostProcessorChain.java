package com.ecg.replyts.app.postprocessorchain;

import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.OrderComparator;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;

@Component("postProcessorChain")
public class PostProcessorChain {
    private static final transient Logger LOG = LoggerFactory.getLogger(PostProcessorChain.class);

    @Autowired(required = false)
    private List<PostProcessor> postProcessors = emptyList();

    @PostConstruct
    public void orderPostProcessors() {
        LOG.info("Custom Postprocesors Registered: {}", postProcessors);
        Collections.sort(this.postProcessors, new OrderComparator());
    }

    public void postProcess(MessageProcessingContext context) {
        for (PostProcessor postProcessor : postProcessors) {
            LOG.debug("Applying post-processor {} for message id {}",
                    postProcessor.getClass().getSimpleName(), context.getMessageId());

            postProcessor.postProcess(context);

            if (context.isTerminated()) {
                LOG.debug("Stop processing mail after post-processor {}",
                        postProcessor.getClass().getSimpleName());
                break;
            }
        }
    }

    public void setPostProcessors(List<PostProcessor> postProcessors) {
        this.postProcessors = postProcessors;
    }
}
