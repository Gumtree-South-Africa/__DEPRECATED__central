package com.ecg.au.gumtree.replyts.threading;

/**
 * @author mdarapour
 */
public class MessageThreadingConfig {
    private int order;

    public MessageThreadingConfig(int order) {
        this.order = order;
    }

    public int getOrder() {
        return order;
    }
}
