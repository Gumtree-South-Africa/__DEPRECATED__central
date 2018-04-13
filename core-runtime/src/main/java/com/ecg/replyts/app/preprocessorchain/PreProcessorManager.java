package com.ecg.replyts.app.preprocessorchain;

import com.ecg.replyts.core.api.processing.MessageProcessingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.OrderComparator;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PreProcessorManager {

    private static final transient Logger LOG = LoggerFactory.getLogger(PreProcessorManager.class);

    @Autowired
    private List<PreProcessor> preProcessors;

    @PostConstruct
    public void orderPreProcessors() {
        Collections.sort(this.preProcessors, new OrderComparator());
        LOG.info("PreProcessors registered: {}", preProcessors.stream().map(Object::getClass).map(Class::getName).collect(Collectors.joining(", ")));
    }

    public void preProcess(MessageProcessingContext context) {
        for(PreProcessor preProcessor: preProcessors) {
            preProcessor.preProcess(context);

            if (context.isTerminated()) return;
        }
    }

}
