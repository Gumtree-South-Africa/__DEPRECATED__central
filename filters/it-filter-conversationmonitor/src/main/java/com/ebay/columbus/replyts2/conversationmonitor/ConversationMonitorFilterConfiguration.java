package com.ebay.columbus.replyts2.conversationmonitor;

import com.ecg.replyts.core.api.pluginconfiguration.ComaasPlugin;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@ComaasPlugin
@Configuration
@ComponentScan("com.ebay.columbus.replyts2.conversationmonitor")
public class ConversationMonitorFilterConfiguration { }