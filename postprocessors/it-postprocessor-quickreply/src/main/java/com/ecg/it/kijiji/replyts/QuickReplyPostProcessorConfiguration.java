package com.ecg.it.kijiji.replyts;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ComaasPlugin
@Configuration
@Import(QuickReplyPostProcessor.class)
public class QuickReplyPostProcessorConfiguration { }