package com.ecg.replyts.app.postprocessorchain;

import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.OrderComparator;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

@Component("postProcessorChain")
public class PostProcessorChain {
    private static final transient Logger LOG = LoggerFactory.getLogger(PostProcessorChain.class);

    @Autowired(required = false)
    private List<PostProcessor> postProcessors = emptyList();

    @PostConstruct
    public void orderPostProcessors() {
        // !!! mutating sort operation, not thread safe !!! ???
        this.postProcessors.sort(new OrderComparator());
        String processorNames = postProcessors.stream().map(Object::getClass).map(Class::getName).collect(Collectors.joining(", "));
        LOG.info("PostProcessors registered: {}", processorNames);
        PostProcessorTracer.info(this, "constructed with postProcessors: {}", processorNames);
    }

    public void postProcess(MessageProcessingContext context) {
        for (PostProcessor postProcessor : postProcessors) {
            if (postProcessor.isApplicable(context)) {
                PostProcessorTracer.logPostProcessorApplicable(postProcessor);

                LOG.debug("Applying post-processor {} for message id {}", postProcessor.getClass().getSimpleName(), context.getMessageId());

                postProcessor.postProcess(context);

                if (context.isTerminated()) {
                    LOG.debug("Stop processing mail after post-processor {}", postProcessor.getClass().getSimpleName());
                    break;
                }

            } else {
                PostProcessorTracer.logPostProcessorNotApplicable(postProcessor);
                LOG.debug("NOT applying post-processor {} for message id {}", postProcessor.getClass().getSimpleName(), context.getMessageId());
            }
        }
    }

    public void setPostProcessors(List<PostProcessor> postProcessors) {
        this.postProcessors = postProcessors;
    }
}
